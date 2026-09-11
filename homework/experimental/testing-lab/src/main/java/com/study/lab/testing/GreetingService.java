package com.study.lab.testing;

import org.springframework.stereotype.Service;

/**
 * 被测的业务逻辑：一个极简的问候服务。
 * 这里故意保留一处业务规则（name 为空时抛异常），供测试覆盖分支。
 */
@Service
public class GreetingService {

    /**
     * 生成问候语。name 为空或空白时抛 IllegalArgumentException。
     */
    public String greet(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 不能为空");
        }
        return "Hello, " + name;
    }
}
