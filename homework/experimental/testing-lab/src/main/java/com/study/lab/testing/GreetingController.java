package com.study.lab.testing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 被测的 Web 层：一个极简问候接口。
 * GET /api/greeting?name=xxx -> {"message":"Hello, xxx"}
 *
 * <p>观察点：纯单元测试能直接 new 出本类调用 greet()，但测不到
 * 路径映射、参数绑定、JSON 序列化；这些要靠 @WebMvcTest + MockMvc。
 */
@RestController
@RequestMapping("/api/greeting")
public class GreetingController {

    private final GreetingService greetingService;

    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping
    public GreetingResponse greeting(@RequestParam String name) {
        return new GreetingResponse(greetingService.greet(name));
    }

    /** 简单响应体，用于验证 JSON 序列化。 */
    public record GreetingResponse(String message) {}
}
