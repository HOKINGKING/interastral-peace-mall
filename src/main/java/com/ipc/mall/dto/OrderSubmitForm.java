package com.ipc.mall.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 下单表单（PRD §5.11 / docs/api.md F25）。
 * <p>
 * 两个入口共用：{@code cartIds} 非空走购物车结算，否则用 {@code itemId + quantity} 立即购买。
 * 价格一律以数据库为准，这里<b>不接收</b>任何价格字段。
 */
@Data
public class OrderSubmitForm {

    /** 防重复提交 Token（来自确认页隐藏域，取值后立即失效） */
    private String submitToken;

    /** 收货地址 id，必须属于当前账号 */
    private Long addressId;

    /** 购物车行 id，逗号分隔；为空表示立即购买 */
    private String cartIds;

    /** 立即购买：SKU id */
    private Long itemId;

    /** 立即购买：数量，1–99 */
    private Integer quantity;

    /** 订单备注，选填，≤200 字 */
    @Size(max = 200, message = "订单备注最多 200 字")
    private String remark;
}
