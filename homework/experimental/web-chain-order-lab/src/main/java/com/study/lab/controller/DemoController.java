package com.study.lab.controller;

import com.study.lab.service.DemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 观察用接口。请求 /demo 会依次穿过 Filter → Interceptor → Aspect → Controller → Service。
 * 传 ?fail=true 可让接口抛异常，用于观察异常时各环节是否执行。
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping
    public String demo(@RequestParam(defaultValue = "false") boolean fail) {
        if (fail) {
            throw new IllegalStateException("故意抛出，用于观察异常路径");
        }
        return demoService.hello();
    }
}
