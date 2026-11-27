package com.ipc.mall.service;

/**
 * 超时未支付自动关单（PRD §8.2 T4，P1）。
 * <p>
 * 由 {@code OrderTimeoutJob} 定时调用：扫描下单超过 pay-timeout-minutes 仍为
 * PENDING_PAYMENT 的订单，逐单条件关闭并回补库存。
 */
public interface OrderTimeoutService {

    /**
     * 关掉一批超时未支付订单。
     *
     * @return 本轮真正关闭的订单数（并发下已被支付/取消的订单不计）
     */
    int closeTimeoutOrders();
}
