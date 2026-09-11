package com.study.mall.product.service;


import com.study.mall.product.common.exception.ResourceConflictException;
import com.study.mall.product.common.exception.ResourceNotFoundException;
import com.study.mall.product.common.result.Page;
import com.study.mall.product.config.ProductConfig;
import com.study.mall.product.domain.dto.CreateProductRequest;
import com.study.mall.product.domain.dto.ProductQuery;
import com.study.mall.product.domain.entity.ProductEntity;
import com.study.mall.product.domain.vo.ProductSummaryVO;
import com.study.mall.product.repository.InMemoryProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    InMemoryProductRepository inMemoryProductRepository;

    @Mock
    ProductConfig productConfig;

    @InjectMocks
    ProductService productService;

    @Test
    @DisplayName("创建商品时SKU已存在")
    void creat_product_when_sku_exist() {
       // 打桩，相当于自己new InMemoryProductRepository 写好前置条件
       when(inMemoryProductRepository.existsBySku("SKU-1")).thenReturn(true);

       var request = new CreateProductRequest("安卓机", "SKU-1", new BigDecimal("500"), "手机");

        assertThatThrownBy(() -> productService.create(request)).isInstanceOf(ResourceConflictException.class).hasMessageContaining("SKU 已存在");

        verify(inMemoryProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("创建商品时SKU不存在，正常保存")
    void creat_product_when_sku_not_exist() {
        // 打桩，相当于自己new InMemoryProductRepository 写好前置条件
        when(inMemoryProductRepository.existsBySku("SKU-1")).thenReturn(false);


        // 让 save 原样返回传入的实体(常用手法)
        when(inMemoryProductRepository.save(any(ProductEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateProductRequest("安卓机", "SKU-1", new BigDecimal("500"), "手机");

        var vo = productService.create(request);
        assertThat(vo.sku()).isEqualTo("SKU-1");
        verify(inMemoryProductRepository).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("getById：id 不存在时抛 ResourceNotFoundException")
    void get_by_id_when_not_found() {
        when(inMemoryProductRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("list：请求 size 超过配置上限时被截断到上限")
    void list_size_truncated_to_max() {
        // 上限设为 2，请求 size=10，期望实际返回不超过 2 条
        when(productConfig.pagination()).thenReturn(new ProductConfig.Pagination(2));

        LocalDateTime now = LocalDateTime.now();
        List<ProductEntity> data = List.of(
                new ProductEntity(1L, "商品1", "SKU-1", new BigDecimal("10"), "手机", now, now),
                new ProductEntity(2L, "商品2", "SKU-2", new BigDecimal("20"), "手机", now, now),
                new ProductEntity(3L, "商品3", "SKU-3", new BigDecimal("30"), "手机", now, now),
                new ProductEntity(4L, "商品4", "SKU-4", new BigDecimal("40"), "手机", now, now),
                new ProductEntity(5L, "商品5", "SKU-5", new BigDecimal("50"), "手机", now, now)
        );
        when(inMemoryProductRepository.findAll()).thenReturn(data);

        Page<ProductSummaryVO> page = productService.list(new ProductQuery(0, 10, null));

        // size 被截断到上限 2，首页只返回 2 条，但 total 仍为全部 5 条
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.items()).hasSize(2);
        assertThat(page.total()).isEqualTo(5);
    }

    @Test
    @DisplayName("deleteById：委托 repository.deleteById 执行删除")
    void delete_by_id_delegates_to_repository() {
        productService.deleteById(7L);

        verify(inMemoryProductRepository).deleteById(7L);
    }

}
