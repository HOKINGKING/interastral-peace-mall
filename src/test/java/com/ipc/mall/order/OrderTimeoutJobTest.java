package com.ipc.mall.order;

import com.ipc.mall.service.OrderTimeoutService;
import com.ipc.mall.task.OrderTimeoutJob;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 定时关单入口：调用服务、异常不外抛（不能让调度线程被打断）。
 */
class OrderTimeoutJobTest {

    @Test
    void 正常执行时调用关单服务() {
        OrderTimeoutService service = mock(OrderTimeoutService.class);
        when(service.closeTimeoutOrders()).thenReturn(2);

        new OrderTimeoutJob(service).closeTimeoutOrders();

        verify(service).closeTimeoutOrders();
    }

    @Test
    void 服务抛异常时任务不向外抛() {
        OrderTimeoutService service = mock(OrderTimeoutService.class);
        when(service.closeTimeoutOrders()).thenThrow(new RuntimeException("db down"));

        // 不抛异常即通过；下一轮定时任务会重试
        new OrderTimeoutJob(service).closeTimeoutOrders();

        verify(service).closeTimeoutOrders();
    }
}
