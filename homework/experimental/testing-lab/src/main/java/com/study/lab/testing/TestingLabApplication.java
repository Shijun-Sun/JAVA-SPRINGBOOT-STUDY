package com.study.lab.testing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试实验脚手架启动类。
 * 本工程用于对比"纯单元测试"与"@WebMvcTest 切片测试"，属于知识点实验，非正式交付。
 */
@SpringBootApplication
public class TestingLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestingLabApplication.class, args);
    }
}
