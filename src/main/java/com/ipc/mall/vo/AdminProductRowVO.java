package com.ipc.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 后台 SPU 列表行（PRD §6.3）：一页一次聚合出最低价、销量、SKU 数、总库存，避免 N+1。
 */
@Data
public class AdminProductRowVO {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long id;

    private String name;

    private String mainImage;

    private Long categoryId;

    /** 「一级 / 二级」拼接 */
    private String categoryName;

    private BigDecimal minPrice;

    private Integer sales;

    private Integer skuCount;

    private Integer totalStock;

    /** 1 上架 / 0 下架 */
    private Integer status;

    private LocalDateTime updateTime;

    public boolean isOnSale() {
        return status != null && status == 1;
    }

    public String getUpdateTimeText() {
        return updateTime == null ? "" : MINUTE.format(updateTime);
    }

    public String getMinPriceText() {
        return minPrice == null ? "—" : minPrice.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
