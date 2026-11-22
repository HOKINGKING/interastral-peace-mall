package com.ipc.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 确认订单（结算页）模型（PRD §5.11 / docs/api.md F24）。
 */
@Data
public class CheckoutVO {

    private List<OrderItemVO> items = new ArrayList<>();

    /** 收货地址卡片列表（默认地址置顶） */
    private List<AddressVO> addresses = new ArrayList<>();

    /** 默认选中的地址；没有默认地址时取第一条 */
    private Long defaultAddressId;

    private BigDecimal goodsAmount = BigDecimal.ZERO;

    /** 运费本期固定 0.00（字段已落库 freight_amount） */
    private BigDecimal freightAmount = BigDecimal.ZERO;

    /** 应付 = 商品总额 + 运费 */
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String goodsAmountText;

    private String freightAmountText;

    private String totalAmountText;

    private int totalQuantity;

    /** 防重复提交 Token，页面隐藏域回传 */
    private String submitToken;

    /** 购物车来源时回传（逗号分隔），立即购买时为空 */
    private String cartIds;

    private Long itemId;

    private Integer quantity;

    /** 非空表示不能结算（已下架/库存不足/没选商品），页面禁用提交按钮 */
    private String blocked;
}
