# logstash+elasticsearch+kibana分布式集群日志收集系统

logstash+elasticsearch+kibana分布式集群日志收集系统，超级详细！最快上手！

| 名称          | 版本       |
| ------------- | ---------- |
| kibana        | 5.6.8      |
| elasticsearch | 5.6.8      |
| kafka         | 2.11-1.0.0 |
| logstash      | 6.2.3      |

### 启动kafka，建立topic

- cd kafka解压后的目录内

- 运行bin/zookeeper-server-start.sh config/zookeeper.properties &

- 运行bin/kafka-server-start.sh config/server.properties &

- 创建topic

  bin/kafka-topics.sh --create --zookeeper localhost:2181 --topic 你的topic名称 --partirion 1 --replication-factor 1

- 检查topic

  bin/kafka-topics.sh --describe --zookeeper localhost:2181 --topic 你的topic名称

### 配置log4j2，搭建项目

- 配置maven依赖

  ```xml
  <dependency>    
      <groupId>org.springframework.boot</groupId>    
      <artifactId>spring-boot-starter</artifactId>    
      <!-- 去除springboot自带的日志 -->       
      <exclusions>        
      	<exclusion>            
      	<groupId>org.springframework.boot</groupId>            
      	<artifactId>spring-boot-starter-logging</artifactId>        
      	</exclusion>    
    	</exclusions>
  </dependency>
  <dependency>    
  	<groupId>org.springframework.boot</groupId>   
      <artifactId>spring-boot-starter-log4j2</artifactId>
  </dependency>
  ```

- 新建日志测试类

  ```java
  @RestController
  @RequestMapping("/api")
  public class DemoController {
  	
  	private static Logger logger = LoggerFactory.getLogger(DemoApplication.class);
  	
  	@RequestMapping("/hello")
  	public void hello() throws Exception {
  		logger.info("invoke hello!");
  		throw new Exception("test invoke hello occur exception");
  	}
  	
  }
  ```

- 配置log4j2

  - application.yml

    ```
    server.port=8800
    logging.config=classpath:log4j-spring-kafka.xml
    ```

  - 新建log4j-spring-kafka.xml

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <!--日志级别以及优先级排序: OFF > FATAL > ERROR > WARN > INFO > DEBUG > TRACE > ALL -->
    <Configuration status="OFF">
        <Appenders>
            <!-- 输出控制台日志的配置 -->
            <Console name="console" target="SYSTEM_OUT">
                <ThresholdFilter level="DEBUG" onMatch="ACCEPT" onMismatch="DENY"/>
                <!-- 输出日志的格式 -->
                <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss SSS} [%t] %-5level %logger{36} - %msg%n"/>
            </Console>
            <Kafka name="Kafka" topic="你的服务器收集日志的topic名称">
                <PatternLayout pattern="%date %message"/>
                <Property name="bootstrap.servers">kafka服务器ip:kafka监听端口</Property>
            </Kafka>
        </Appenders>
    
        <Loggers>
            <Root level="ALL">
                <AppenderRef ref="Kafka"/>
                <AppenderRef ref="console"/>
            </Root>
            <Logger name="org.apache.kafka" level="INFO"/>
            <logger name="org.springframework" level="INFO"/>
        </Loggers>
    </Configuration>
    ```

- 服务器执行下面的命令消费指定topic，kafka

  ```linux
  bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic serverlogs  --from-beginning
  ```

- 启动项目，此时应该可以看到kafka已经打印出来了启动日志！

### 安装logstash，elasticsearch，kibana

- 网上教程极多，就不介绍如何安装qaq

- 由于自己的机子是2g的内存，所以说用的满满当当，这几个组件都挺能抢内存的，elasticsearch默认就要2g内存......个人最终是elasticsearch（400m）+logstash（400m）+其他，这个注意调整不然申请内存不够必报错

- 安装的时候基本的错误基本都能看到错误描述，也好解决

  - 配置文件格式错
  - 扩展标识符数量ulimit -Hn 66666
  - 分配权限 chmod -R 777 文件夹
  - elasticsearch不能在root用户下运行，要新建用户newuser，然后 su newuser，拷贝安装包到/usr目录下cd到指定目录启动elasticsearch
  - ......

- 系统架构图

  ![image-20200530233819220](http://118.31.11.163:6868/files/struct.png)

- logstash配置文件

  ```
  input {
      kafka {
          bootstrap_servers => ["118.31.11.163:9092"]
          topics => ["serverlogs"]
  	type => "log4j-json"
      }
  }
  output {
      stdout {
        codec => rubydebug
      }
      elasticsearch {
          hosts => ["118.31.11.163:9200"]
          index => "applogstash-%{+YYYY.MM.dd.HH}"
      }
  }
  ```

### 效果展示

本地启动项目

![image-20200530235207128](http://118.31.11.163:6868/files/image-20200530235326592.png)

Management里填写 applogstash-* 就可以看到日志！（不要勾选@timestamp！）

![image-20200530235326592](http://118.31.11.163:6868/files/image-20200530235207128.png)

### TODO list

- 加上过滤器，把数据标准格式化
- 能不能把nginx日志配置到里面，也不难，读取file就可以
- 集群化，其实就是几个zk相互注册，几个kafka提供消息服务，配置好ip和参数就可以
- 集成到已有项目，尽量无侵入

### 更新

- logstash配置文件

  ```
  input {
      kafka {
          bootstrap_servers => ["118.31.11.163:9092"]
          topics => ["serverlogs"]
          type => "json"
      }
  }
  filter {
      grok {
          patterns_dir => ["/root/logstash-6.2.3/patterns"]
          match => {
              "message" => "%{TIMESTAMP_ISO8601:timestamp}\[%{MY_THREAD:threadname}\]\s%{LOGLEVEL:loglevel}\s\s\[%{MY_METHOD:method}\]\s-\s%{MY_MSG:information}"
          }
      }
  }
  output {
      stdout {
        codec => rubydebug
      }
      elasticsearch {
          hosts => ["118.31.11.163:9200"]
          index => "applogstash-%{+YYYY.MM.dd.HH}"
      }
  }
  ```

- log4j2配置

  ```xml
          <Kafka name="Kafka" topic="serverlogs">
              <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS}[%t] %-5level [%l] - %msg"/>
              <Property name="bootstrap.servers">118.31.11.163:9092</Property>
          </Kafka>
  ```