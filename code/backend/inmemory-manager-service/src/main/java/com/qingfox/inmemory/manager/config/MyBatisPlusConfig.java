package com.qingfox.inmemory.manager.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.qingfox.inmemory.manager.mapper")
public class MyBatisPlusConfig {
}
