package com.ipc.mall.vo;

import com.ipc.mall.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单明细视图：确认页（下单前校验）、订单详情（快照展示）共用。
 * <p>
 * 详情页展示的是 {@code order_item} 的<b>快照</b>，不要回查商品（可能已改价或下架）。
 */
@Data
public class OrderItemVO {

    private Long itemId;

    private Long productId;

    private String productName;

    private String spec;

    private BigDecimal price;

    /** 两位小数字符串，模板与 JS 直接显示 */
    private String priceText;

    private Integer quantity;

    /** 图片快照，空则前端用占位块 */
    private String image;

    private BigDecimal subtotal;

    private String subtotalText;

    /** 下单前校验用：当前库存；详情页无意义（快照） */
    private Integer stock;

    /** 下单前校验用：商品是否仍在售；详情页恒为 true */
    private boolean available = true;

    /** 下单前校验用：库存是否够；详情页恒为 true */
    private boolean stockEnough = true;

    /** 订单快照 → 视图（详情页） */
    public static OrderItemVO of(OrderItem line) {
        OrderItemVO vo = new OrderItemVO();
        vo.setItemId(line.getItemId());
        vo.setProductId(line.getProductId());
        vo.setProductName(line.getProductName());
        vo.setSpec(line.getSpec());
        vo.setPrice(scale(line.getPrice()));
        vo.setPriceText(text(vo.getPrice()));
        vo.setQuantity(line.getQuantity());
        vo.setImage(line.getImage());
        vo.setSubtotal(scale(line.lineAmount()));
        vo.setSubtotalText(text(vo.getSubtotal()));
        vo.setStock(null);
        return vo;
    }

    /** 小计 = 单价 × 数量 */
    public void computeSubtotal() {
        BigDecimal amount = getPrice() == null || getQuantity() == null
                ? BigDecimal.ZERO
                : getPrice().multiply(BigDecimal.valueOf(getQuantity()));
        setSubtotal(scale(amount));
        setSubtotalText(text(getSubtotal()));
    }

    public static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static String text(BigDecimal value) {
        return scale(value).toPlainString();
    }
}
