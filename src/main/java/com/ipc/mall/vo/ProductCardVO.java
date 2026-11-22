package com.ipc.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品卡数据（首页推荐、列表、分类页、看了又看共用）。
 * <p>
 * 由 {@code ProductMapper.selectCardPage} 聚合 SKU 得出最低价与总库存，
 * 不在模板里做 N+1 查询（PRD §11.3）。
 */
@Data
public class ProductCardVO {

    private Long id;

    private String name;

    private String mainImage;

    private Long categoryId;

    /** 二级分类名，卡片 meta 行展示 */
    private String categoryName;

    /** 该 SPU 下所有未删除 SKU 的最低售价 */
    private BigDecimal minPrice;

    /** 总库存 = SUM(item.stock)，0 表示售罄 */
    private Integer totalStock;

    /** SKU 数量，0 的 SPU 不应出现在前台 */
    private Integer skuCount;

    /** 累计已售件数（product.sales，PRD §7.6） */
    private Integer sales;

    private LocalDateTime updateTime;

    public boolean isSoldOut() {
        return totalStock == null || totalStock <= 0;
    }
}
