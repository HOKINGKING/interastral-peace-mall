package com.ipc.mall.order;

import com.ipc.mall.entity.Order;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.service.OrderService;
import com.ipc.mall.service.impl.OrderTimeoutServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 超时关单（PRD §8.2 T4）：候选扫描、逐单关闭、并发跳过与单笔失败隔离。
 */
class OrderTimeoutServiceImplTest {

    private OrderMapper orderMapper;
    private OrderService orderService;
    private OrderTimeoutServiceImpl service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderService = mock(OrderService.class);
        service = new OrderTimeoutServiceImpl(orderMapper, orderService, 30, 100);
    }

    @Test
    void 没有超时订单时返回零() {
        when(orderMapper.selectTimeoutCandidates(any(), anyInt())).thenReturn(List.of());

        assertEquals(0, service.closeTimeoutOrders());
        verify(orderService, never()).closeTimeoutOrder(any());
    }

    @Test
    void 逐单关闭并跳过已被支付的订单() {
        when(orderMapper.selectTimeoutCandidates(any(), anyInt())).thenReturn(List.of(order(1L), order(2L)));
        when(orderService.closeTimeoutOrder(1L)).thenReturn(true);
        // 第二轮扫描到但用户刚好支付了，条件更新 0 行
        when(orderService.closeTimeoutOrder(2L)).thenReturn(false);

        assertEquals(1, service.closeTimeoutOrders());

        verify(orderService).closeTimeoutOrder(1L);
        verify(orderService).closeTimeoutOrder(2L);
    }

    @Test
    void 单笔失败不影响其它订单() {
        when(orderMapper.selectTimeoutCandidates(any(), anyInt())).thenReturn(List.of(order(1L), order(2L)));
        when(orderService.closeTimeoutOrder(1L)).thenThrow(new RuntimeException("db down"));
        when(orderService.closeTimeoutOrder(2L)).thenReturn(true);

        assertEquals(1, service.closeTimeoutOrders());

        verify(orderService).closeTimeoutOrder(2L);
    }

    @Test
    void 扫描截止时间等于当前时间减去超时分钟() {
        LocalDateTime earliest = LocalDateTime.now().minusMinutes(30).minusSeconds(2);
        when(orderMapper.selectTimeoutCandidates(any(), anyInt())).thenReturn(List.of());

        service.closeTimeoutOrders();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderMapper).selectTimeoutCandidates(captor.capture(), eq(100));
        LocalDateTime deadline = captor.getValue();
        assertFalse(deadline.isBefore(earliest), "截止时间不应早于「现在 - 30 分钟」");
        assertFalse(deadline.isAfter(LocalDateTime.now().minusMinutes(30).plusSeconds(2)),
                "截止时间不应晚于「现在 - 30 分钟」");
    }

    private Order order(Long id) {
        Order order = new Order();
        order.setId(id);
        return order;
    }
}
