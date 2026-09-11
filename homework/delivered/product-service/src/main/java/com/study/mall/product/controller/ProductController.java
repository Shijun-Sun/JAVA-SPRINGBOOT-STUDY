package com.study.mall.product.controller;

import com.study.mall.product.common.result.Page;
import com.study.mall.product.common.result.Result;
import com.study.mall.product.domain.dto.CreateProductRequest;
import com.study.mall.product.domain.dto.ProductQuery;
import com.study.mall.product.domain.dto.UpdateProductRequest;
import com.study.mall.product.domain.vo.ProductSummaryVO;
import com.study.mall.product.domain.vo.ProductVO;
import com.study.mall.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品 REST API。
 * 路径前缀 /api/v1/products，HTTP 状态码遵循 REST 语义。
 * 异常由 GlobalExceptionHandler 统一处理，Controller 只负责正常流程。
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 分页查询商品列表，支持关键字过滤。
     * 查询参数（page/size/keyword）由 Spring MVC 直接绑定到 ProductQuery record，
     * 无需 @RequestBody 或 @RequestParam：这是查询字符串到对象的默认绑定行为。
     * 返回摘要视图 ProductSummaryVO（不含 sku）。
     */
    @GetMapping
    public Result<Page<ProductSummaryVO>> list(ProductQuery query) {
        return Result.ok(productService.list(query));
    }

    /**
     * 查询单个商品。
     * 不存在时 Service 抛 ResourceNotFoundException，由 GlobalExceptionHandler 返回 404。
     */
    @GetMapping("/{id}")
    public Result<ProductVO> findById(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    /** 创建商品，成功返回 201 Created。SKU 冲突时抛 ResourceConflictException → 409。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<ProductVO> create(
            @Validated @RequestBody CreateProductRequest request) {
        return Result.ok(productService.create(request));
    }

    /**
     * 更新商品，整体替换。
     * 不存在时 Service 抛 ResourceNotFoundException，由 GlobalExceptionHandler 返回 404。
     */
    @PutMapping("/{id}")
    public Result<ProductVO> update(
            @PathVariable Long id,
            @Validated @RequestBody UpdateProductRequest request) {
        return Result.ok(productService.update(id, request));
    }

    /** 删除商品，成功返回 204 No Content，无响应体 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        productService.deleteById(id);
    }
}
