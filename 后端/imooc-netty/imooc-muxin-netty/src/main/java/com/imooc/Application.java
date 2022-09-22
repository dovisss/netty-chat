package com.imooc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
// 扫描mybatis mapper包路径
@MapperScan(basePackages="com.imooc.mapper")
// 扫描 所有需要的包, 包含一些自用的工具类包 所在的路径
@ComponentScan(basePackages= {"com.imooc","org.n3r.idworker"})
public class Application {

	@Bean
	public SpringUtil getSpringUtil() {
		return new SpringUtil();
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
