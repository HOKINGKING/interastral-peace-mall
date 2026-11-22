package com.ipc.mall.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SKU 视图（详情页规格切换与 {@code GET /product/{id}/skus} 共用）。
 */
@Data
public class SkuVO {

    private Long id;

    private String spec;

    private BigDecimal price;

    /** 两位小数字符串，给模板的 data-price 用（避免在属性里调用格式化函数） */
    @JsonIgnore
    private String priceText;

    private Integer stock;

    /** 规格图，空则回退 SPU 主图 */
    private String image;

    public boolean isSoldOut() {
        return stock == null || stock <= 0;
    }
}
