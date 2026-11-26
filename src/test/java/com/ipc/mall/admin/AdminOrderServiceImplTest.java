package com.ipc.mall.admin;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.ReceiverForm;
import com.ipc.mall.dto.ShipForm;
import com.ipc.mall.entity.Account;
import com.ipc.mall.entity.Order;
import com.ipc.mall.entity.OrderItem;
import com.ipc.mall.mapper.AccountMapper;
import com.ipc.mall.mapper.OrderItemMapper;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.service.impl.AdminOrderServiceImpl;
import com.ipc.mall.vo.OrderDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 后台订单管理（PRD §6.5 A22–A26）：发货状态机、改收货信息、终态删除、日期区间。
 */
class AdminOrderServiceImplTest {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private AccountMapper accountMapper;
    private AdminOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        accountMapper = mock(AccountMapper.class);
        service = new AdminOrderServiceImpl(orderMapper, orderItemMapper, accountMapper);
    }

    // ------------------------------------------------------------ 列表

    @Test
    void 列表把日期区间转成半开区间并过滤非法状态() {
        service.page(" 2026111 ", " xingye ", "NOT_A_STATUS", LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 25), 2, 10);

        verify(orderMapper).selectAdminPage(any(), eq("2026111"), eq("xingye"), isNull(),
                eq(LocalDateTime.of(2026, 11, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 11, 26, 0, 0)));
    }

    @Test
    void 列表只按状态名过滤() {
        service.page(null, null, "PAID", null, null, 1, 10);

        verify(orderMapper).selectAdminPage(any(), isNull(), isNull(), eq("PAID"), isNull(), isNull());
    }

    // ------------------------------------------------------------ 详情

    @Test
    void 详情带上下单用户名() {
        when(orderMapper.selectById(2L)).thenReturn(order(2L, OrderStatus.COMPLETED));
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(item(2L)));
        when(accountMapper.selectById(1L)).thenReturn(account(1L, "xingye", "星野"));

        OrderDetailVO detail = service.detail(2L);

        assertEquals("xingye", detail.getUsername());
        assertEquals("已完成", detail.getStatusDesc());
        assertEquals(1, detail.getItems().size());
        assertEquals("IPC202611160001", detail.getTrackingNo());
        assertTrue(detail.isCanDelete());
    }

    @Test
    void 详情不存在返回三千零一() {
        when(orderMapper.selectById(9L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> service.detail(9L));
        assertEquals(ResultCode.ORDER_NOT_FOUND.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 发货

    @Test
    void 发货成功走条件更新() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, OrderStatus.PAID));
        when(orderMapper.markShipped(eq(5L), eq("星际速运"), eq("IPC202611170002"),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1);

        service.ship(5L, ship("星际速运", "IPC202611170002"));

        verify(orderMapper).markShipped(eq(5L), eq("星际速运"), eq("IPC202611170002"),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void 重复发货或非已支付返回三千零二() {
        when(orderMapper.selectById(4L)).thenReturn(order(4L, OrderStatus.SHIPPED));

        BizException exception = assertThrows(BizException.class, () -> service.ship(4L, ship("星际速运", "X1")));

        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
        verify(orderMapper, never()).markShipped(any(), any(), any(), any(), any());
    }

    @Test
    void 待支付订单不能发货() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, OrderStatus.PENDING_PAYMENT));

        BizException exception = assertThrows(BizException.class, () -> service.ship(1L, ship("星际速运", "X1")));
        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
    }

    @Test
    void 缺少物流信息返回四百() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, OrderStatus.PAID));

        BizException noCourier = assertThrows(BizException.class, () -> service.ship(5L, ship("  ", "X1")));
        assertTrue(noCourier.getMessage().contains("物流公司"));

        BizException noTracking = assertThrows(BizException.class, () -> service.ship(5L, ship("星际速运", " ")));
        assertTrue(noTracking.getMessage().contains("物流单号"));
    }

    @Test
    void 发货条件更新零行返回三千零二() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, OrderStatus.PAID));
        when(orderMapper.markShipped(any(), any(), any(), any(), any())).thenReturn(0);

        BizException exception = assertThrows(BizException.class, () -> service.ship(5L, ship("星际速运", "X1")));
        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 改收货信息

    @Test
    void 改收货信息时备注为空表示保留原值() {
        Order order = order(5L, OrderStatus.PAID);
        order.setRemark("原备注");
        when(orderMapper.selectById(5L)).thenReturn(order);
        when(orderMapper.updateReceiver(eq(5L), eq("陈开拓"), eq("13800000002"), eq("新地址 1 号"),
                eq("原备注"), any(LocalDateTime.class))).thenReturn(1);

        service.updateReceiver(5L, receiver("陈开拓", "13800000002", "新地址 1 号", null));

        verify(orderMapper).updateReceiver(eq(5L), eq("陈开拓"), eq("13800000002"), eq("新地址 1 号"),
                eq("原备注"), any(LocalDateTime.class));
    }

    @Test
    void 改收货信息时备注空串表示清空() {
        Order order = order(5L, OrderStatus.PAID);
        order.setRemark("原备注");
        when(orderMapper.selectById(5L)).thenReturn(order);
        when(orderMapper.updateReceiver(eq(5L), any(), any(), any(), isNull(), any(LocalDateTime.class)))
                .thenReturn(1);

        service.updateReceiver(5L, receiver("陈开拓", "13800000002", "新地址 1 号", "  "));

        verify(orderMapper).updateReceiver(eq(5L), any(), any(), any(), isNull(), any(LocalDateTime.class));
    }

    @Test
    void 改收货信息手机号非法返回四百() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, OrderStatus.PAID));

        BizException exception = assertThrows(BizException.class,
                () -> service.updateReceiver(5L, receiver("陈开拓", "123", "地址", null)));

        assertTrue(exception.getMessage().contains("手机号"));
        verify(orderMapper, never()).updateReceiver(any(), any(), any(), any(), any(), any());
    }

    @Test
    void 订单不存在时改收货信息返回三千零一() {
        when(orderMapper.selectById(9L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class,
                () -> service.updateReceiver(9L, receiver("陈开拓", "13800000002", "地址", null)));
        assertEquals(ResultCode.ORDER_NOT_FOUND.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 删除

    @Test
    void 终态订单可以删除() {
        when(orderMapper.selectById(3L)).thenReturn(order(3L, OrderStatus.CANCELLED));

        service.remove(3L);

        verify(orderMapper).deleteById(3L);
    }

    @Test
    void 非终态订单删除返回三千零二() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, OrderStatus.PAID));

        BizException exception = assertThrows(BizException.class, () -> service.remove(5L));

        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
        verify(orderMapper, never()).deleteById(anyLong());
    }

    // ------------------------------------------------------------ 夹具

    private ShipForm ship(String courier, String tracking) {
        ShipForm form = new ShipForm();
        form.setCourierCompany(courier);
        form.setTrackingNo(tracking);
        return form;
    }

    private ReceiverForm receiver(String name, String phone, String detail, String remark) {
        ReceiverForm form = new ReceiverForm();
        form.setReceiverName(name);
        form.setReceiverPhone(phone);
        form.setReceiverDetail(detail);
        form.setRemark(remark);
        return form;
    }

    private Order order(Long id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setAccountId(1L);
        order.setOrderNo("2026111720150" + id);
        order.setStatus(status.name());
        order.setTotalAmount(new java.math.BigDecimal("127.00"));
        order.setFreightAmount(java.math.BigDecimal.ZERO);
        order.setReceiverName("星野");
        order.setReceiverPhone("13800000001");
        order.setReceiverProvince("湖南省");
        order.setReceiverCity("长沙市");
        order.setReceiverDistrict("岳麓区");
        order.setReceiverDetail("星轨大道 88 号");
        order.setCourierCompany("星际速运");
        order.setTrackingNo("IPC202611160001");
        order.setCreateTime(LocalDateTime.now().minusDays(1));
        return order;
    }

    private OrderItem item(Long orderId) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setItemId(1L);
        item.setProductId(1L);
        item.setProductName("星际和平水杯 · 500ml");
        item.setSpec("500ml 星白");
        item.setPrice(new java.math.BigDecimal("59.00"));
        item.setQuantity(1);
        return item;
    }

    private Account account(Long id, String username, String nickname) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setNickname(nickname);
        return account;
    }
}
