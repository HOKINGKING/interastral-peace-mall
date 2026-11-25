package com.ipc.mall.order;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.common.constant.ProductStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.OrderSubmitForm;
import com.ipc.mall.entity.Address;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Order;
import com.ipc.mall.entity.OrderItem;
import com.ipc.mall.entity.PaymentRecord;
import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.AddressMapper;
import com.ipc.mall.mapper.ItemMapper;
import com.ipc.mall.mapper.OrderItemMapper;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.mapper.PaymentRecordMapper;
import com.ipc.mall.mapper.ProductMapper;
import com.ipc.mall.service.AddressService;
import com.ipc.mall.service.CartService;
import com.ipc.mall.service.impl.OrderServiceImpl;
import com.ipc.mall.vo.AddressVO;
import com.ipc.mall.vo.CartItemVO;
import com.ipc.mall.vo.CheckoutVO;
import com.ipc.mall.vo.OrderDetailVO;
import com.ipc.mall.vo.OrderItemVO;
import com.ipc.mall.vo.OrderSummaryVO;
import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 结算下单（PRD §5.11）。
 * <p>
 * 重点：金额以数据库现价为准、校验顺序与错误码、下单/明细/扣库存/清购物车在同一事务里、
 * 扣库存 0 行要整单失败。
 */
class OrderServiceImplTest {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private ItemMapper itemMapper;
    private ProductMapper productMapper;
    private PaymentRecordMapper paymentRecordMapper;
    private CartService cartService;
    private AddressService addressService;
    private AddressMapper addressMapper;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        itemMapper = mock(ItemMapper.class);
        productMapper = mock(ProductMapper.class);
        paymentRecordMapper = mock(PaymentRecordMapper.class);
        cartService = mock(CartService.class);
        addressService = mock(AddressService.class);
        addressMapper = mock(AddressMapper.class);
        orderService = new OrderServiceImpl(orderMapper, orderItemMapper, itemMapper, productMapper,
                paymentRecordMapper, cartService, addressService, addressMapper);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return 1;
        });
    }

    // ------------------------------------------------------------ 确认页

    @Test
    void 购物车结算按数据库现价组装明细() {
        when(cartService.listByIds(1L, List.of(1L, 2L))).thenReturn(List.of(
                cart(1L, 6L, 2), cart(2L, 8L, 1)));
        when(itemMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                sku(6L, 4L, "39.00", 240, 3), sku(8L, 6L, "49.00", 88, 5)));
        when(productMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                product(4L, "驾驶员徽章套装（6 枚）", ProductStatus.ON_SALE),
                product(6L, "命运星图海报 · A2", ProductStatus.ON_SALE)));
        when(addressService.list(1L)).thenReturn(List.of(addressVO(1L, true)));

        CheckoutVO vo = orderService.checkout(1L, "1,2", null, null);

        assertEquals(2, vo.getItems().size());
        assertNull(vo.getBlocked());
        assertEquals("39.00", vo.getItems().get(0).getPriceText());
        assertEquals("78.00", vo.getItems().get(0).getSubtotalText());
        assertEquals("127.00", vo.getGoodsAmountText());
        assertEquals("127.00", vo.getTotalAmountText());
        assertEquals("0.00", vo.getFreightAmountText());
        assertEquals(3, vo.getTotalQuantity());
        assertEquals(1L, vo.getDefaultAddressId());
        assertEquals("1,2", vo.getCartIds());
    }

    @Test
    void 立即购买只组装一条且不带cartIds() {
        when(itemMapper.selectById(5L)).thenReturn(sku(5L, 3L, "299.00", 12, 1));
        when(productMapper.selectById(3L)).thenReturn(product(3L, "舰桥模型 · 纪念版", ProductStatus.ON_SALE));

        CheckoutVO vo = orderService.checkout(1L, null, 5L, 2);

        assertEquals(1, vo.getItems().size());
        assertEquals("598.00", vo.getTotalAmountText());
        assertNull(vo.getCartIds());
        assertEquals(5L, vo.getItemId());
        assertEquals(2, vo.getQuantity());
    }

    @Test
    void 确认页在商品下架时给出提示但不抛异常() {
        when(cartService.listByIds(1L, List.of(1L))).thenReturn(List.of(cart(1L, 6L, 1)));
        when(itemMapper.selectBatchIds(anyCollection())).thenReturn(List.of(sku(6L, 4L, "39.00", 240, 0)));
        when(productMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                product(4L, "驾驶员徽章套装（6 枚）", ProductStatus.OFF_SALE)));
        when(addressService.list(1L)).thenReturn(List.of());

        CheckoutVO vo = orderService.checkout(1L, "1", null, null);

        assertNotNull(vo.getBlocked());
        assertTrue(vo.getBlocked().contains("已下架"));
        assertFalse(vo.getItems().get(0).isAvailable());
        assertNull(vo.getDefaultAddressId());
    }

    @Test
    void 确认页在库存不足时给出提示() {
        when(cartService.listByIds(1L, List.of(1L))).thenReturn(List.of(cart(1L, 6L, 5)));
        when(itemMapper.selectBatchIds(anyCollection())).thenReturn(List.of(sku(6L, 4L, "39.00", 2, 0)));
        when(productMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                product(4L, "驾驶员徽章套装（6 枚）", ProductStatus.ON_SALE)));

        CheckoutVO vo = orderService.checkout(1L, "1", null, null);

        assertNotNull(vo.getBlocked());
        assertTrue(vo.getBlocked().contains("库存不足"));
        assertFalse(vo.getItems().get(0).isStockEnough());
    }

    @Test
    void 没选商品时确认页提示空购物车() {
        when(cartService.listByIds(1L, List.of(9L))).thenReturn(List.of());

        CheckoutVO vo = orderService.checkout(1L, "9", null, null);

        assertTrue(vo.getItems().isEmpty());
        assertEquals(ResultCode.CART_EMPTY.getMessage(), vo.getBlocked());
    }

    // ------------------------------------------------------------ 提交校验顺序

    @Test
    void 没有可结算商品返回三千零五() {
        when(cartService.listByIds(1L, List.of(9L))).thenReturn(List.of());

        BizException exception = assertThrows(BizException.class,
                () -> orderService.submit(1L, form("9", null, null, "备注", 1L)));
        assertEquals(ResultCode.CART_EMPTY.getCode(), exception.getCode());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void 商品已下架返回两千零一() {
        mockCartLine(6L, 1, ProductStatus.OFF_SALE, 240);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L));

        BizException exception = assertThrows(BizException.class,
                () -> orderService.submit(1L, form("1", null, null, null, 1L)));
        assertEquals(ResultCode.PRODUCT_OFF_SALE.getCode(), exception.getCode());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void 库存不足返回两千零二() {
        mockCartLine(6L, 3, ProductStatus.ON_SALE, 2);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L));

        BizException exception = assertThrows(BizException.class,
                () -> orderService.submit(1L, form("1", null, null, null, 1L)));
        assertEquals(ResultCode.STOCK_NOT_ENOUGH.getCode(), exception.getCode());
        verify(itemMapper, never()).deductStock(anyLong(), anyInt(), anyInt());
    }

    @Test
    void 地址不属于本人返回三千零四() {
        mockCartLine(6L, 1, ProductStatus.ON_SALE, 240);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 2L));

        BizException exception = assertThrows(BizException.class,
                () -> orderService.submit(1L, form("1", null, null, null, 1L)));
        assertEquals(ResultCode.NO_PERMISSION.getCode(), exception.getCode());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void 备注超过两百字返回四百() {
        mockCartLine(6L, 1, ProductStatus.ON_SALE, 240);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L));

        String longRemark = "备".repeat(201);
        BizException exception = assertThrows(BizException.class,
                () -> orderService.submit(1L, form("1", null, null, longRemark, 1L)));
        assertEquals(ResultCode.FAIL.getCode(), exception.getCode());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    // ------------------------------------------------------------ 下单成功

    @Test
    void 提交成功落订单明细并扣库存清购物车() {
        mockCartLine(6L, 2, ProductStatus.ON_SALE, 240);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L));
        when(itemMapper.deductStock(6L, 2, 3)).thenReturn(1);

        Long orderId = orderService.submit(1L,
                form("1", null, null, "   WORKDAY 送达  ", 1L));

        assertEquals(100L, orderId);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(orderCaptor.capture());
        Order order = orderCaptor.getValue();
        assertEquals(1L, order.getAccountId());
        assertEquals(OrderStatus.PENDING_PAYMENT.name(), order.getStatus());
        assertEquals("78.00", order.getTotalAmount().toPlainString());
        assertEquals("0.00", order.getFreightAmount().toPlainString());
        assertEquals("WORKDAY 送达", order.getRemark());
        // 收货信息是快照
        assertEquals("星野", order.getReceiverName());
        assertEquals("13900000001", order.getReceiverPhone());
        assertEquals("湖南省", order.getReceiverProvince());
        assertEquals("星轨大道 88 号", order.getReceiverDetail());
        assertNotNull(order.getOrderNo());
        assertEquals(18, order.getOrderNo().length());

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemMapper).insert(itemCaptor.capture());
        OrderItem line = itemCaptor.getValue();
        assertEquals(100L, line.getOrderId());
        assertEquals(6L, line.getItemId());
        assertEquals(4L, line.getProductId(), "销量归集用的是 product_id");
        assertEquals("驾驶员徽章套装（6 枚）", line.getProductName());
        assertEquals("6 枚装", line.getSpec());
        assertEquals("39.00", line.getPrice().toPlainString());
        assertEquals(2, line.getQuantity());

        verify(itemMapper).deductStock(6L, 2, 3);
        verify(cartService).remove(1L, 1L);
    }

    @Test
    void 扣库存零行时抛两千零二() {
        mockCartLine(6L, 1, ProductStatus.ON_SALE, 240);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L));
        when(itemMapper.deductStock(6L, 1, 3)).thenReturn(0);

        BizException exception = assertThrows(BizException.class,
                () -> orderService.submit(1L, form("1", null, null, null, 1L)));
        assertEquals(ResultCode.STOCK_NOT_ENOUGH.getCode(), exception.getCode());
        // 事务由 @Transactional 回滚：这里只验证异常被抛出、购物车没被清
        verify(cartService, never()).remove(any(), any());
    }

    @Test
    void 立即购买不经过购物车() {
        when(itemMapper.selectById(5L)).thenReturn(sku(5L, 3L, "299.00", 12, 1));
        when(productMapper.selectById(3L)).thenReturn(product(3L, "舰桥模型 · 纪念版", ProductStatus.ON_SALE));
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L));
        when(itemMapper.deductStock(5L, 1, 1)).thenReturn(1);

        orderService.submit(1L, form(null, 5L, 1, null, 1L));

        verify(cartService, never()).listByIds(any(), any());
        verify(cartService, never()).remove(any(), any());
        verify(itemMapper).deductStock(5L, 1, 1);
    }

    @Test
    void 立即购买数量越界返回四百() {
        when(itemMapper.selectById(5L)).thenReturn(sku(5L, 3L, "299.00", 12, 1));
        when(productMapper.selectById(3L)).thenReturn(product(3L, "舰桥模型 · 纪念版", ProductStatus.ON_SALE));

        BizException exception = assertThrows(BizException.class,
                () -> orderService.submit(1L, form(null, 5L, 100, null, 1L)));
        assertEquals(ResultCode.FAIL.getCode(), exception.getCode());
    }

    @Test
    void 空备注归一化为null() {
        mockCartLine(6L, 1, ProductStatus.ON_SALE, 240);
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L));
        when(itemMapper.deductStock(6L, 1, 3)).thenReturn(1);

        orderService.submit(1L, form("1", null, null, "   ", 1L));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        assertNull(captor.getValue().getRemark());
    }

    // ------------------------------------------------------------ 支付 / 取消 / 收货

    @Test
    void 支付成功写payTime并按SPU分组累加销量() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 1L, OrderStatus.PENDING_PAYMENT));
        when(orderMapper.markPaid(eq(1L), eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                line(1L, 6L, 4L, 2),
                line(1L, 8L, 6L, 1)));

        orderService.pay(1L, 1L);

        verify(orderMapper).markPaid(eq(1L), eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(productMapper).increaseSales(4L, 2);
        verify(productMapper).increaseSales(6L, 1);

        ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordMapper).insert(paymentCaptor.capture());
        PaymentRecord payment = paymentCaptor.getValue();
        assertEquals("PAY20261117201501", payment.getTransactionNo());
        assertEquals("149.00", payment.getAmount().toPlainString());
        assertEquals(PaymentRecord.METHOD_MOCK, payment.getPayMethod());
        assertEquals(PaymentRecord.STATUS_SUCCESS, payment.getStatus());
        assertEquals(1L, payment.getOrderId());
        assertNotNull(payment.getPayTime());
    }

    @Test
    void 重复支付幂等不重复加销量() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 1L, OrderStatus.PAID));

        orderService.pay(1L, 1L);

        verify(orderMapper, never()).markPaid(any(), any(), any(), any());
        verify(productMapper, never()).increaseSales(any(), any());
        verify(paymentRecordMapper, never()).insert(any(PaymentRecord.class));
    }

    @Test
    void 条件更新零行但已是已支付仍按幂等处理() {
        when(orderMapper.selectById(1L))
                .thenReturn(order(1L, 1L, OrderStatus.PENDING_PAYMENT), order(1L, 1L, OrderStatus.PAID));
        when(orderMapper.markPaid(eq(1L), eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0);

        orderService.pay(1L, 1L);

        verify(productMapper, never()).increaseSales(any(), any());
    }

    @Test
    void 已取消订单支付返回三千零二() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 1L, OrderStatus.CANCELLED));

        BizException exception = assertThrows(BizException.class, () -> orderService.pay(1L, 1L));
        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
        verify(orderMapper, never()).markPaid(any(), any(), any(), any());
    }

    @Test
    void 支付时订单不存在返回三千零一() {
        when(orderMapper.selectById(1L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> orderService.pay(1L, 1L));
        assertEquals(ResultCode.ORDER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void 支付别人的订单返回三千零三() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 2L, OrderStatus.PENDING_PAYMENT));

        BizException exception = assertThrows(BizException.class, () -> orderService.pay(1L, 1L));
        assertEquals(ResultCode.ORDER_NOT_OWNED.getCode(), exception.getCode());
    }

    @Test
    void 取消订单回补库存() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 1L, OrderStatus.PENDING_PAYMENT));
        when(orderMapper.markCancelled(eq(1L), eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                line(1L, 6L, 4L, 2),
                line(1L, 8L, 6L, 1)));

        orderService.cancel(1L, 1L);

        verify(itemMapper).restoreStock(6L, 2);
        verify(itemMapper).restoreStock(8L, 1);
    }

    @Test
    void 已支付订单取消返回三千零二且不回补库存() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 1L, OrderStatus.PAID));

        BizException exception = assertThrows(BizException.class, () -> orderService.cancel(1L, 1L));
        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
        verify(itemMapper, never()).restoreStock(any(), any());
    }

    @Test
    void 取消时条件更新零行不回补库存() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 1L, OrderStatus.PENDING_PAYMENT));
        when(orderMapper.markCancelled(eq(1L), eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0);

        BizException exception = assertThrows(BizException.class, () -> orderService.cancel(1L, 1L));
        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
        verify(itemMapper, never()).restoreStock(any(), any());
    }

    @Test
    void 已发货订单确认收货成功() {
        when(orderMapper.selectById(4L)).thenReturn(order(4L, 1L, OrderStatus.SHIPPED));
        when(orderMapper.markCompleted(eq(4L), eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);

        orderService.receive(1L, 4L);

        verify(orderMapper).markCompleted(eq(4L), eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void 已支付订单确认收货返回三千零二() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 2L, OrderStatus.PAID));

        BizException exception = assertThrows(BizException.class, () -> orderService.receive(2L, 5L));
        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
        verify(orderMapper, never()).markCompleted(any(), any(), any(), any());
    }

    // ------------------------------------------------------------ 订单列表 / 详情 / 删除

    @Test
    void 列表按状态筛选并组装卡片() {
        Order pending = order(1L, 1L, OrderStatus.PENDING_PAYMENT);
        pending.setTotalAmount(new BigDecimal("148.00"));
        Order shipped = order(4L, 1L, OrderStatus.SHIPPED);
        shipped.setCourierCompany("星际速运");
        shipped.setTrackingNo("IPC202611170002");

        Page<Order> raw = new Page<>(1, 6, 2);
        raw.setRecords(List.of(pending, shipped));
        when(orderMapper.selectPage(any(), any())).thenReturn(raw);
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                line(1L, 1L, 1L, 1), line(1L, 3L, 2L, 1),
                line(4L, 5L, 3L, 1)));

        IPage<OrderSummaryVO> page = orderService.pageOrders(1L, "PENDING_PAYMENT", 1, 6);

        assertEquals(2, page.getRecords().size());
        assertEquals(2, page.getTotal());
        OrderSummaryVO first = page.getRecords().get(0);
        assertEquals("20261117201501", first.getOrderNo());
        assertEquals("待支付", first.getStatusDesc());
        assertEquals("badge-pending", first.getStatusClass());
        assertEquals(2, first.getTotalQuantity());
        assertEquals("测试商品", first.getFirstProductName());
        assertTrue(first.isCanPay());
        assertTrue(first.isCanCancel());
        assertFalse(first.isCanReceive());
        assertFalse(first.isCanDelete());

        OrderSummaryVO second = page.getRecords().get(1);
        assertEquals("badge-shipped", second.getStatusClass());
        assertTrue(second.isCanReceive());
        assertTrue(second.isCanComplaint());
        assertEquals("IPC202611170002", second.getTrackingNo());
    }

    @Test
    void 详情组装时间线金额与收货快照() {
        Order completed = order(2L, 1L, OrderStatus.COMPLETED);
        completed.setReceiverName("星野");
        completed.setReceiverPhone("13800000001");
        completed.setReceiverProvince("湖南省");
        completed.setReceiverCity("长沙市");
        completed.setReceiverDistrict("岳麓区");
        completed.setReceiverDetail("星轨大道 88 号");
        completed.setRemark("放前台");
        completed.setFreightAmount(new BigDecimal("0.00"));
        completed.setTotalAmount(new BigDecimal("129.00"));
        completed.setCreateTime(LocalDateTime.now().minusDays(2));
        completed.setPayTime(LocalDateTime.now().minusDays(2).plusMinutes(1));
        completed.setShipTime(LocalDateTime.now().minusDays(1));
        completed.setFinishTime(LocalDateTime.now());
        completed.setCourierCompany("星际速运");
        completed.setTrackingNo("IPC202611160001");

        when(orderMapper.selectById(2L)).thenReturn(completed);
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                line(2L, 7L, 5L, 1)));
        when(paymentRecordMapper.selectOne(any(Wrapper.class))).thenReturn(paymentRecord(2L));

        OrderDetailVO detail = orderService.detail(1L, 2L);

        assertEquals("已完成", detail.getStatusDesc());
        assertEquals("badge-completed", detail.getStatusClass());
        assertEquals("138****0001", detail.getReceiverPhoneMasked());
        assertEquals("湖南省 长沙市 岳麓区 星轨大道 88 号", detail.getReceiverAddress());
        assertEquals("放前台", detail.getRemark());
        assertEquals(1, detail.getItems().size());
        assertEquals("39.00", detail.getGoodsAmountText());
        assertEquals("129.00", detail.getTotalAmountText());
        assertTrue(detail.isHasPayment());
        assertEquals("PAY20261117201502", detail.getPaymentTransactionNo());
        assertEquals("模拟支付", detail.getPaymentMethod());
        assertEquals("129.00", detail.getPaymentAmountText());
        assertNotNull(detail.getPaymentTimeText());
        assertTrue(detail.isCanDelete());
        assertFalse(detail.isCanPay());
        // 下单 / 支付 / 发货 / 完成 四个节点都已完成
        assertEquals(4, detail.getTimeline().size());
        assertTrue(detail.getTimeline().stream().allMatch(OrderDetailVO.TimelineNode::isDone));
        assertTrue(detail.getTimeline().get(2).getLabel().contains("星际速运"));
    }

    @Test
    void 取消订单的时间线只有两条() {
        Order cancelled = order(3L, 1L, OrderStatus.CANCELLED);
        cancelled.setCreateTime(LocalDateTime.now().minusDays(2));
        cancelled.setCancelTime(LocalDateTime.now().minusDays(1));
        when(orderMapper.selectById(3L)).thenReturn(cancelled);
        when(orderItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        OrderDetailVO detail = orderService.detail(1L, 3L);

        assertEquals(2, detail.getTimeline().size());
        assertEquals("订单已取消，库存已回补", detail.getTimeline().get(1).getLabel());
        assertTrue(detail.isCanDelete());
    }

    @Test
    void 看别人的订单详情返回三千零三() {
        when(orderMapper.selectById(1L)).thenReturn(order(1L, 2L, OrderStatus.PAID));

        BizException exception = assertThrows(BizException.class, () -> orderService.detail(1L, 1L));
        assertEquals(ResultCode.ORDER_NOT_OWNED.getCode(), exception.getCode());
    }

    @Test
    void 终态订单可以删除且不回补库存() {
        when(orderMapper.selectById(2L)).thenReturn(order(2L, 1L, OrderStatus.COMPLETED));

        orderService.remove(1L, 2L);

        verify(orderMapper).deleteById(2L);
        verify(itemMapper, never()).restoreStock(any(), any());
    }

    @Test
    void 非终态订单删除返回三千零二() {
        when(orderMapper.selectById(8L)).thenReturn(order(8L, 1L, OrderStatus.PENDING_PAYMENT));

        BizException exception = assertThrows(BizException.class, () -> orderService.remove(1L, 8L));
        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
        verify(orderMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void 删除别人的订单返回三千零三() {
        when(orderMapper.selectById(8L)).thenReturn(order(8L, 2L, OrderStatus.COMPLETED));

        BizException exception = assertThrows(BizException.class, () -> orderService.remove(1L, 8L));
        assertEquals(ResultCode.ORDER_NOT_OWNED.getCode(), exception.getCode());
        verify(orderMapper, never()).deleteById(any(Long.class));
    }

    // ------------------------------------------------------------ 夹具

    private void mockCartLine(Long itemId, int quantity, int productStatus, int stock) {
        when(cartService.listByIds(1L, List.of(1L))).thenReturn(List.of(cart(1L, itemId, quantity)));
        when(itemMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                sku(itemId, 4L, "39.00", stock, 3)));
        when(productMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                product(4L, "驾驶员徽章套装（6 枚）", productStatus)));
    }

    private OrderSubmitForm form(String cartIds, Long itemId, Integer quantity, String remark, Long addressId) {
        OrderSubmitForm form = new OrderSubmitForm();
        form.setSubmitToken("token");
        form.setCartIds(cartIds);
        form.setItemId(itemId);
        form.setQuantity(quantity);
        form.setRemark(remark);
        form.setAddressId(addressId);
        return form;
    }

    private Order order(Long id, Long accountId, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setAccountId(accountId);
        order.setStatus(status.name());
        order.setOrderNo("2026111720150" + id);
        order.setTotalAmount(new BigDecimal("149.00"));
        order.setFreightAmount(new BigDecimal("0.00"));
        return order;
    }

    private OrderItem line(Long orderId, Long itemId, Long productId, int quantity) {
        OrderItem line = new OrderItem();
        line.setOrderId(orderId);
        line.setItemId(itemId);
        line.setProductId(productId);
        line.setProductName("测试商品");
        line.setSpec("测试规格");
        line.setPrice(new BigDecimal("39.00"));
        line.setQuantity(quantity);
        return line;
    }

    private CartItemVO cart(Long cartId, Long itemId, int quantity) {        CartItemVO vo = new CartItemVO();
        vo.setCartId(cartId);
        vo.setItemId(itemId);
        vo.setQuantity(quantity);
        vo.setAvailable(true);
        return vo;
    }

    private Item sku(Long id, Long productId, String price, int stock, int version) {
        Item item = new Item();
        item.setId(id);
        item.setProductId(productId);
        item.setSpec("6 枚装");
        item.setPrice(new BigDecimal(price));
        item.setStock(stock);
        item.setVersion(version);
        return item;
    }

    private Product product(Long id, String name, int status) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setStatus(status);
        return product;
    }

    private PaymentRecord paymentRecord(Long orderId) {
        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId(orderId);
        payment.setOrderNo("2026111720150" + orderId);
        payment.setAccountId(1L);
        payment.setAmount(new BigDecimal("129.00"));
        payment.setPayMethod(PaymentRecord.METHOD_MOCK);
        payment.setStatus(PaymentRecord.STATUS_SUCCESS);
        payment.setTransactionNo("PAY2026111720150" + orderId);
        payment.setPayTime(LocalDateTime.now().minusDays(2).plusMinutes(1));
        return payment;
    }

    private Address address(Long id, Long accountId) {        Address address = new Address();
        address.setId(id);
        address.setAccountId(accountId);
        address.setReceiver("星野");
        address.setPhone("13900000001");
        address.setProvince("湖南省");
        address.setCity("长沙市");
        address.setDistrict("岳麓区");
        address.setDetail("星轨大道 88 号");
        address.setIsDefault(1);
        return address;
    }

    private AddressVO addressVO(Long id, boolean defaultFlag) {
        AddressVO vo = new AddressVO();
        vo.setId(id);
        vo.setReceiver("星野");
        vo.setPhoneMasked("139****0001");
        vo.setFullAddress("湖南省 长沙市 岳麓区 星轨大道 88 号");
        vo.setDefaultFlag(defaultFlag);
        return vo;
    }
}
