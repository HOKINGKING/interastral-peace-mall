package com.ipc.mall.common.constant;

/**
 * 商品状态常量。
 * <p>
 * 数据库 product.status 是 VARCHAR(16) + CHECK 约束（见 db/schema.sql）。
 * Java 侧刻意不用枚举类型做实体字段，而是用 String + 常量的方式：
 * <ul>
 *     <li>实体字段保持 String，MyBatis-Plus 不需要额外的类型处理器；</li>
 *     <li>Thymeleaf 里直接比较字符串即可；</li>
 *     <li>数据库、Java、前端三处的字面量完全一致，不会出现枚举 ordinal 错位。</li>
 * </ul>
 */
public final class ProductStatus {

    /** 下架：不出现在用户端列表，可被后台"上架" */
    public static final String DRAFT = "DRAFT";

    /** 上架：可被浏览、加购、下单（仍需 stock > 0） */
    public static final String ON_SALE = "ON_SALE";

    private ProductStatus() {
    }

    public static boolean isOnSale(String status) {
        return ON_SALE.equals(status);
    }

    public static boolean isDraft(String status) {
        return DRAFT.equals(status);
    }

    public static String descOf(String status) {
        return switch (String.valueOf(status)) {
            case ON_SALE -> "已上架";
            case DRAFT -> "已下架";
            default -> status;
        };
    }
}
