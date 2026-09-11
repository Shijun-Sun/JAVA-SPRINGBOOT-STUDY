package com.study.mall.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端集成测试：启动完整 Spring 上下文（含 Servlet 过滤器链），
 * 依赖 src/test/resources/application.yml 提供的测试配置。
 * <p>
 * 这里只测「切片测试测不到」的真实链路：
 * <ul>
 *   <li>TraceIdFilter 通过 FilterRegistrationBean 注册在 Servlet 容器层，
 *       @WebMvcTest 只加载 MVC 层、不会挂上这类 Filter，因此 X-Trace-Id 响应头只能在集成测试里验证；</li>
 *   <li>错误契约经过「真实 Controller → Service → GlobalExceptionHandler」完整走一遍，
 *       不依赖对 Service 的打桩。</li>
 * </ul>
 * 任务一/二已覆盖的 Service 逻辑与 Web 契约细节，这里不重复。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTest {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("GET 列表：200 且响应头带 X-Trace-Id（证明 TraceIdFilter 真的挂上链路）")
    void list_returns_ok_with_trace_id_header() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(header().exists(TRACE_ID_HEADER));
    }

    @Test
    @DisplayName("传入自定义 X-Trace-Id：响应头回传同一个值（复用而非重新生成）")
    void custom_trace_id_is_reused() throws Exception {
        String customTraceId = "it-trace-0001";

        mockMvc.perform(get("/api/v1/products").header(TRACE_ID_HEADER, customTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string(TRACE_ID_HEADER, customTraceId));
    }

    @Test
    @DisplayName("GET 不存在商品：端到端 404 + RESOURCE_NOT_FOUND（错误契约经真实链路）")
    void get_missing_product_returns_404_contract() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
