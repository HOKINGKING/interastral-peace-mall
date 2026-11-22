package com.ipc.mall.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车页聚合视图。
 * <p>
 * {@code totalCount} 是商品总件数（数量求和），用于顶部角标与「已选 N 件」的初始值；
 * {@code totalAmount} 只是「全部可结算商品」的合计，页面上的合计以勾选项为准，
 * 由前端实时重算（PRD §5.9：合计仅统计勾选项）。
 */
@Data
public class CartVO {

    private List<CartItemVO> items = new ArrayList<>();

    /** 商品总件数 = Σ quantity（含失效行，失效行也还占着购物车） */
    private int totalCount;

    /** 可结算商品合计（前端会在勾选变化时覆盖为勾选合计） */
    private BigDecimal totalAmount = BigDecimal.ZERO;
}
