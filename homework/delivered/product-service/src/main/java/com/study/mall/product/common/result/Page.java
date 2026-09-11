package com.study.mall.product.common.result;

import java.util.List;

/**
 * 通用分页响应对象，包裹在 {@link Result} 里对外返回。
 *
 * @param items      当前页数据
 * @param total      过滤后的总记录数
 * @param page       当前页码，从 0 开始
 * @param size       每页大小（已按上限截断后的实际值）
 * @param totalPages 总页数
 */
public record Page<T>(
        List<T> items,
        long total,
        int page,
        int size,
        int totalPages
) {

    /**
     * 由当前页数据、总数、页码和页大小构造分页对象，自动计算总页数。
     * size 非正时总页数记为 0，避免除零。
     */
    public static <T> Page<T> of(List<T> items, long total, int page, int size) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new Page<>(items, total, page, size, totalPages);
    }
}
