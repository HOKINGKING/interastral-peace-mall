package com.ipc.mall.common.constant;

/**
 * 商品上下架状态（product.status 为 TINYINT，PRD §7.3）。
 * <p>
 * 注意：库存、价格、乐观锁都在 SKU 层（item 表），不在 SPU 上。
 */
public final class ProductStatus {

    /** 上架：可被浏览（仍需其下某个 SKU 有库存才能下单） */
    public static final int ON_SALE = 1;

    /** 下架：不出现在用户端列表 */
    public static final int OFF_SALE = 0;

    private ProductStatus() {
    }

    public static boolean isOnSale(Integer status) {
        return status != null && status == ON_SALE;
    }

    public static String descOf(Integer status) {
        return isOnSale(status) ? "已上架" : "已下架";
    }
}
