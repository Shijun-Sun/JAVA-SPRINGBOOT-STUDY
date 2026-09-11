package com.study.mall.product.domain.dto;

/**
 * 商品列表查询参数，由 Spring MVC 直接绑定查询字符串到 record。
 * <p>
 * page / size 用包装类型 {@link Integer} 而非原始 int：查询参数缺省时 Spring 绑定为
 * null，原始 int 无法承接 null 会触发类型转换失败（返回 400），包装类型才能让"缺省即
 * 使用默认值"成立。紧凑构造器统一归一化，构造完成后 page/size 一定非空。
 * <p>
 * size 的上限依赖运行期配置 product.pagination.max-size，无法在 record 内读取，
 * 因此上限截断放在 Service 层处理，本处只保证入参落在合法基础区间。
 *
 * @param page    页码，从 0 开始，缺省或非法时归一化为 0
 * @param size    每页大小，缺省或非正时归一化为 20
 * @param keyword 关键字，按名称或分类模糊匹配，可为空
 */
public record ProductQuery(Integer page, Integer size, String keyword) {

    public ProductQuery {
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 20;
        }
    }
}
