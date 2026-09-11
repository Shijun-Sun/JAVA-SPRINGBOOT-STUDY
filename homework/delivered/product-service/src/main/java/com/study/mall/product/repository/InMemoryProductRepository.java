package com.study.mall.product.repository;

import com.study.mall.product.domain.entity.ProductEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存的商品存储实现。
 * M02 接入 MyBatis-Plus 时只替换本类，Service 层不需要改动。
 */
@Repository
public class InMemoryProductRepository {

    private final Map<Long, ProductEntity> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    /**
     * 保存新商品，生成自增 id，忽略入参的 id 字段。
     */
    public ProductEntity save(ProductEntity entity) {
        long id = idSequence.getAndIncrement();
        ProductEntity toStore = new ProductEntity(
                id,
                entity.name(),
                entity.sku(),
                entity.price(),
                entity.category(),
                entity.createdAt(),
                entity.updatedAt()
        );
        store.put(id, toStore);
        return toStore;
    }

    public Optional<ProductEntity> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * 返回快照列表，防止外部修改内部状态。
     */
    public Collection<ProductEntity> findAll() {
        return List.copyOf(store.values());
    }

    /**
     * 整体替换，id 不存在时返回 empty。
     */
    public Optional<ProductEntity> update(Long id, ProductEntity entity) {
        if (!store.containsKey(id)) {
            return Optional.empty();
        }
        store.put(id, entity);
        return Optional.of(entity);
    }

    /**
     * 删除商品，存在并成功删除返回 true，不存在返回 false。
     */
    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }

    public boolean existsBySku(String sku) {
        return store.values().stream().anyMatch(e -> e.sku().equals(sku));
    }
}
