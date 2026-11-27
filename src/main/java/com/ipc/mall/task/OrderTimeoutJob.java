package com.ipc.mall.task;

import com.ipc.mall.service.OrderTimeoutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 超时未支付自动关单的定时入口（PRD §8.2 T4，P1）。
 * <p>
 * 默认每 60 秒扫一次、启动后延迟 30 秒再跑；间隔与延迟都可在 application.yml 调。
 * 任务本身只做异常兜底，真正的扫描与关单逻辑在 {@link OrderTimeoutService}。
 * <p>
 * {@code mall.order.timeout-enabled=false} 时不注册该 Bean（测试环境用）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mall.order", name = "timeout-enabled",
        havingValue = "true", matchIfMissing = true)
public class OrderTimeoutJob {

    private final OrderTimeoutService orderTimeoutService;

    public OrderTimeoutJob(OrderTimeoutService orderTimeoutService) {
        this.orderTimeoutService = orderTimeoutService;
    }

    @Scheduled(fixedDelayString = "${mall.order.close-check-interval-ms:60000}",
            initialDelayString = "${mall.order.close-check-initial-delay-ms:30000}")
    public void closeTimeoutOrders() {
        try {
            int closed = orderTimeoutService.closeTimeoutOrders();
            if (closed > 0) {
                log.info("定时关单: 本轮关闭 {} 笔超时未支付订单", closed);
            }
        } catch (Exception e) {
            // 定时任务抛异常会把调度线程的后续执行打断，这里必须兜住
            log.error("定时关单任务执行失败，将在下一轮重试", e);
        }
    }
}
