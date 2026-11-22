package com.ipc.mall.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车行视图（PRD §5.9 展示口径）。
 * <p>
 * 勾选状态<b>不在这里</b>：cart 表没有 checked 列，勾选是纯前端状态，
 * 结算时把 cartIds 传给下单接口（docs/api.md F24）。
 */
@Data
public class CartItemVO {

    private Long cartId;

    private Long itemId;

    private Long productId;

    private String productName;

    /** SKU 规格名，如「500ml 星白」 */
    private String spec;

    private BigDecimal price;

    /** 两位小数字符串，给模板的 data-price 用 */
    @JsonIgnore
    private String priceText;

    private Integer quantity;

    private Integer stock;

    /** 数量步进器的上限：min(99, max(库存, 当前数量))，避免前端把已超库存的行截断 */
    private Integer maxQty;

    /** SKU 图，空则回退 SPU 主图，两者都空时前端用占位块 */
    private String image;

    /** 小计 = price × quantity，BigDecimal 两位小数 */
    private BigDecimal subtotal;

    @JsonIgnore
    private String subtotalText;

    /** 是否可结算：商品被删 / 下架时为 false，置灰且不参与合计 */
    private boolean available;

    /** 置灰原因，available=true 时为 null */
    private String unavailableReason;
}
