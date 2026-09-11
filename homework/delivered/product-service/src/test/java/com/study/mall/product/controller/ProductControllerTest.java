package com.study.mall.product.controller;

import tools.jackson.databind.ObjectMapper;
import com.study.mall.product.common.exception.ResourceNotFoundException;
import com.study.mall.product.domain.dto.CreateProductRequest;
import com.study.mall.product.domain.vo.ProductVO;
import com.study.mall.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProductController 的 Web 切片测试。
 * 只加载 Web 层（Controller + @RestControllerAdvice + 消息转换器），不加载 Service/Repository。
 * ProductService 用 @MockitoBean 顶替，聚焦验证：路由映射、状态码、请求体校验、错误契约。
 * <p>
 * Boot 4：@WebMvcTest 来自 spring-boot-webmvc-test 模块；@MockBean 已移除，改用 @MockitoBean。
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ProductService productService;

    @Test
    @DisplayName("GET /{id} 命中：200 + SUCCESS + 数据字段正确")
    void get_by_id_hit() throws Exception {
        ProductVO vo = new ProductVO(1L, "iPhone", "SKU-100",
                new BigDecimal("6999.00"), "手机", LocalDateTime.now());
        when(productService.getById(1L)).thenReturn(vo);

        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("iPhone"))
                .andExpect(jsonPath("$.data.sku").value("SKU-100"));
    }

    @Test
    @DisplayName("GET /{id} 未命中：404 + RESOURCE_NOT_FOUND（证明全局异常处理在切片内生效）")
    void get_by_id_not_found() throws Exception {
        when(productService.getById(99L)).thenThrow(new ResourceNotFoundException(99L, "商品"));

        mockMvc.perform(get("/api/v1/products/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST 成功：201 Created + 响应体结构")
    void create_success() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "iPhone", "SKU-100", new BigDecimal("6999.00"), "手机");
        ProductVO vo = new ProductVO(1L, "iPhone", "SKU-100",
                new BigDecimal("6999.00"), "手机", LocalDateTime.now());
        when(productService.create(any(CreateProductRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.sku").value("SKU-100"));
    }

    @Test
    @DisplayName("POST 校验失败：400 + VALIDATION_FAILED + 字段错误明细")
    void create_validation_failed() throws Exception {
        // name 空白、sku 不合法（小写+特殊字符）、price 为 0，三处都违规
        CreateProductRequest invalid = new CreateProductRequest(
                "", "bad sku!", new BigDecimal("0"), "手机");

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.sku").exists())
                .andExpect(jsonPath("$.data.price").exists());
    }

    @Test
    @DisplayName("DELETE /{id}：204 No Content")
    void delete_by_id() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
