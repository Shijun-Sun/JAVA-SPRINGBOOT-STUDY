package com.study.lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 执行顺序观察实验启动类。
 * @EnableAspectJAutoProxy 开启 @AspectJ 切面代理（本实验用 aspectjweaver 直接支持）。
 */
@SpringBootApplication
@EnableAspectJAutoProxy
public class WebChainOrderLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebChainOrderLabApplication.class, args);
    }
}
