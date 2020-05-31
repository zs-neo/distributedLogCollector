/*
 * Author github: https://github.com/zs-neo
 * Author Email: 2931622851@qq.com
 */
package com.zs.demo.controller;

import com.zs.demo.DemoApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zhousheng
 * @version 1.0
 * @since 2020/5/30 15:48
 */
@RestController
@RequestMapping("/api")
public class DemoController {
	
	private static Logger logger = LoggerFactory.getLogger(DemoApplication.class);
	
	@RequestMapping("/hello")
	public void hello() throws Exception {
		logger.debug("invoke hello!");
		logger.info("invoke hello!");
		logger.warn("invoke hello!");
		throw new Exception("test invoke hello occur exception");
	}
	
}