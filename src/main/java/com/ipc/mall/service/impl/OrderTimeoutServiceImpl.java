package com.ipc.mall.service.impl;

import com.ipc.mall.entity.Order;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.service.OrderService;
import com.ipc.mall.service.OrderTimeoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时关单实现（PRD §8.2 T4 / §7.6）。
 * <p>
 * 扫描与关闭分开：本类只负责「找出候选 + 逐单调事务关闭」，
 * 每单的 {@code PENDING_PAYMENT → CANCELLED} 与库存回补由
 * {@link OrderService#closeTimeoutOrder(Long)} 在同一事务里完成。
 * <p>
 * 单笔失败只记 warn 并继续，下一轮定时任务会重新扫描；不会因为一笔脏数据卡住整批。
 */
@Slf4j
@Service
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final int payTimeoutMinutes;
    private final int batchSize;

    public OrderTimeoutServiceImpl(OrderMapper orderMapper,
                                   OrderService orderService,
                                   @Value("${mall.order.pay-timeout-minutes:30}") int payTimeoutMinutes,
                                   @Value("${mall.order.close-batch-size:100}") int batchSize) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.payTimeoutMinutes = payTimeoutMinutes;
        this.batchSize = batchSize;
    }

    @Override
    public int closeTimeoutOrders() {
        int timeoutMinutes = Math.max(1, payTimeoutMinutes);
        int limit = Math.max(1, batchSize);
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);

        List<Order> candidates = orderMapper.selectTimeoutCandidates(deadline, limit);
        if (candidates.isEmpty()) {
            log.debug("超时关单扫描: 没有待关闭订单（deadline={}）", deadline);
            return 0;
        }

        int closed = 0;
        for (Order order : candidates) {
            try {
                if (orderService.closeTimeoutOrder(order.getId())) {
                    closed++;
                }
            } catch (Exception e) {
                log.warn("超时关单失败，跳过本次: orderId={} 原因={}", order.getId(), e.toString());
            }
        }
        if (closed > 0) {
            log.info("超时关单完成: 扫描 {} 单，关闭 {} 单", candidates.size(), closed);
        }
        return closed;
    }
}
