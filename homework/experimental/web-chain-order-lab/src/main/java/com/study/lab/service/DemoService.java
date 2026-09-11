package com.study.lab.service;

import org.springframework.stereotype.Service;

/**
 * 被 AOP 切面观察的目标 Service。
 * 方法体已可运行，实验重点在切面能否切到它、以及执行顺序。
 */
@Service
public class DemoService {

    public String hello() {
        return "hello from DemoService";
    }
}
