package com.study.lab.testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 方式二：@WebMvcTest 切片测试（只加载 Web 层）。
 * 注意：Spring Boot 4 用 @MockitoBean，不是已被移除的 @MockBean。
 *
 * 观察它相比纯单元测试多测到了什么：路径映射、参数绑定、JSON 序列化、状态码。
 */
@WebMvcTest(GreetingController.class)
class GreetingControllerSliceTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GreetingService greetingService;

    @Test
    @DisplayName("GET /api/greeting?name=Kiro 返回 200 和 JSON")
    void greeting_returnsJson() throws Exception {
        // TODO: when(greetingService.greet("Kiro")).thenReturn("Hello, Kiro");
        // TODO: mockMvc.perform(get(...).param("name","Kiro"))
        //          .andExpect(status().isOk())
        //          .andExpect(jsonPath("$.message").value("Hello, Kiro"));
    }

    @Test
    @DisplayName("缺少 name 参数返回 400（参数绑定失败——纯单元测不到）")
    void greeting_missingParam_returns400() throws Exception {
        // TODO: 不传 name，断言 status().isBadRequest()
        // 思考：为什么纯单元测试（直接调方法）测不出这个 400？
    }
}
