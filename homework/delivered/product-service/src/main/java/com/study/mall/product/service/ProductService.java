package com.study.mall.product.service;

import com.study.mall.product.common.exception.ResourceConflictException;
import com.study.mall.product.common.exception.ResourceNotFoundException;
import com.study.mall.product.common.result.Page;
import com.study.mall.product.config.ProductConfig;
import com.study.mall.product.domain.dto.CreateProductRequest;
import com.study.mall.product.domain.dto.ProductQuery;
import com.study.mall.product.domain.dto.UpdateProductRequest;
import com.study.mall.product.domain.entity.ProductEntity;
import com.study.mall.product.domain.vo.ProductSummaryVO;
import com.study.mall.product.domain.vo.ProductVO;
import com.study.mall.product.repository.InMemoryProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final InMemoryProductRepository repository;
    private final ProductConfig productConfig;

    public ProductService(InMemoryProductRepository repository, ProductConfig productConfig) {
        this.repository = repository;
        this.productConfig = productConfig;
    }

    /**
     * 创建商品。SKU 已存在时抛 IllegalArgumentException。
     */
    public ProductVO create(CreateProductRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new ResourceConflictException(request.sku());
        }
        LocalDateTime now = LocalDateTime.now();
        ProductEntity entity = new ProductEntity(
                null,
                request.name(),
                request.sku(),
                request.price(),
                request.category(),
                now,
                now
        );
        return toVO(repository.save(entity));
    }

    public Optional<ProductVO> findById(Long id) {
        return repository.findById(id).map(this::toVO);
    }

    public ProductVO getById(Long id) {
        return findById(id).orElseThrow(() -> new ResourceNotFoundException(id, "商品"));
    }

    /**
     * 返回全部商品视图，供内部使用（如无需分页的场景）。
     */
    public List<ProductVO> findAll() {
        return repository.findAll().stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 分页 + 关键字过滤查询，对外列表接口使用。
     * 分页在内存 List 上截取，keyword 按名称或分类不区分大小写包含匹配。
     * 请求的 size 超过配置上限 product.pagination.max-size 时静默截断，不报错。
     */
    public Page<ProductSummaryVO> list(ProductQuery query) {
        int maxSize = productConfig.pagination().maxSize();
        int size = Math.min(query.size(), maxSize);
        int page = query.page();

        List<ProductEntity> filtered = repository.findAll().stream()
                .filter(entity -> matchesKeyword(entity, query.keyword()))
                .sorted(Comparator.comparing(ProductEntity::id))
                .toList();

        long total = filtered.size();
        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<ProductSummaryVO> items = filtered.subList(from, to).stream()
                .map(this::toSummaryVO)
                .toList();

        return Page.of(items, total, page, size);
    }

    /**
     * keyword 为空返回 true（不过滤）；否则按名称或分类不区分大小写包含匹配。
     */
    private boolean matchesKeyword(ProductEntity entity, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String kw = keyword.toLowerCase();
        boolean nameHit = entity.name() != null && entity.name().toLowerCase().contains(kw);
        boolean categoryHit = entity.category() != null && entity.category().toLowerCase().contains(kw);
        return nameHit || categoryHit;
    }

    /**
     * 更新商品。id 不存在时抛 ResourceNotFoundException。
     */
    public ProductVO update(Long id, UpdateProductRequest request) {
        ProductEntity existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "商品"));

        LocalDateTime now = LocalDateTime.now();
        ProductEntity updated = new ProductEntity(
                id,
                request.name(),
                existing.sku(),         // SKU 不允许修改
                request.price(),
                request.category(),
                existing.createdAt(),   // 创建时间保持不变
                now
        );
        return toVO(repository.update(id, updated).orElseThrow());
    }

    /**
     * 删除商品，幂等，不存在时静默返回。
     */
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    // ---- 私有转换方法 ----

    private ProductVO toVO(ProductEntity entity) {
        return new ProductVO(
                entity.id(),
                entity.name(),
                entity.sku(),
                entity.price(),
                entity.category(),
                entity.createdAt()
        );
    }

    private ProductSummaryVO toSummaryVO(ProductEntity entity) {
        return new ProductSummaryVO(
                entity.id(),
                entity.name(),
                entity.price(),
                entity.category(),
                entity.createdAt()
        );
    }
}
