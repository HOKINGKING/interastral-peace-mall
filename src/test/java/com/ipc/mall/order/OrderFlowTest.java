package com.ipc.mall.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.OrderSubmitForm;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Order;
import com.ipc.mall.entity.PaymentRecord;
import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.ItemMapper;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.mapper.PaymentRecordMapper;
import com.ipc.mall.mapper.ProductMapper;
import com.ipc.mall.service.OrderService;
import com.ipc.mall.vo.OrderDetailVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 交易链路集成测试（PRD §5.11–§5.13、§7.6、AC-16/17/18/47）。
 * <p>
 * 依赖已执行 db/schema.sql + db/data.sql 的库；测试方法带事务，结束自动回滚，
 * 不会污染演示数据（库存、销量、订单、流水都会还原）。
 */
@SpringBootTest
@Transactional
class OrderFlowTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Test
    void 下单扣库存支付写流水且重复支付不重复记账() {
        Item before = itemMapper.selectById(1L);
        int stockBefore = before.getStock();
        int salesBefore = productMapper.selectById(1L).getSales();

        Long orderId = orderService.submit(1L, buy(1L, 2, 1L));

        Order created = orderMapper.selectById(orderId);
        assertNotNull(created);
        assertEquals("PENDING_PAYMENT", created.getStatus());
        assertEquals(stockBefore - 2, itemMapper.selectById(1L).getStock(), "下单应扣减库存");
        assertEquals(0L, paymentCount(orderId), "未支付不应有流水");

        orderService.pay(1L, orderId);

        Order paid = orderMapper.selectById(orderId);
        assertEquals("PAID", paid.getStatus());
        assertNotNull(paid.getPayTime(), "支付成功必须写 pay_time");
        assertEquals(salesBefore + 2, productMapper.selectById(1L).getSales(), "支付成功累加销量");
        assertEquals(1L, paymentCount(orderId), "支付成功应写一条流水");

        PaymentRecord payment = paymentRecord(orderId);
        assertEquals("PAY" + paid.getOrderNo(), payment.getTransactionNo());
        assertEquals("MOCK", payment.getPayMethod());
        assertEquals("SUCCESS", payment.getStatus());
        assertEquals(0, paid.getTotalAmount().compareTo(payment.getAmount()), "流水金额取订单应付");
        assertNotNull(payment.getPayTime());

        OrderDetailVO detail = orderService.detail(1L, orderId);
        assertTrue(detail.isHasPayment());
        assertEquals(payment.getTransactionNo(), detail.getPaymentTransactionNo());

        // 幂等：重复支付不重写时间、不重复加销量、不重复写流水
        orderService.pay(1L, orderId);
        assertEquals(1L, paymentCount(orderId), "重复支付不能新增流水");
        assertEquals(paid.getPayTime(), orderMapper.selectById(orderId).getPayTime(), "pay_time 不应被覆盖");
        assertEquals(salesBefore + 2, productMapper.selectById(1L).getSales());
    }

    @Test
    void 取消回补库存且不可重复取消() {
        Item before = itemMapper.selectById(2L);
        int stockBefore = before.getStock();

        Long orderId = orderService.submit(1L, buy(2L, 1, 1L));
        assertEquals(stockBefore - 1, itemMapper.selectById(2L).getStock());

        orderService.cancel(1L, orderId);

        Order cancelled = orderMapper.selectById(orderId);
        assertEquals("CANCELLED", cancelled.getStatus());
        assertNotNull(cancelled.getCancelTime());
        assertEquals(stockBefore, itemMapper.selectById(2L).getStock(), "取消应回补库存");
        assertEquals(0L, paymentCount(orderId), "取消订单不应有流水");

        BizException twice = assertThrows(BizException.class, () -> orderService.cancel(1L, orderId));
        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), twice.getCode());
    }

    @Test
    void 未发货不能确认收货() {
        Long orderId = orderService.submit(1L, buy(1L, 1, 1L));
        orderService.pay(1L, orderId);

        BizException error = assertThrows(BizException.class, () -> orderService.receive(1L, orderId));
        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), error.getCode(),
                "PAID 直接确认收货必须返回 3002（AC-18）");
    }

    @Test
    void 详情不允许查看他人订单() {
        Long orderId = orderService.submit(1L, buy(1L, 1, 1L));

        BizException error = assertThrows(BizException.class, () -> orderService.detail(2L, orderId));
        assertEquals(ResultCode.ORDER_NOT_OWNED.getCode(), error.getCode(), "越权应返回 3003（AC-19）");
    }

    // ------------------------------------------------------------------ 辅助

    private OrderSubmitForm buy(Long itemId, int quantity, Long addressId) {
        OrderSubmitForm form = new OrderSubmitForm();
        form.setItemId(itemId);
        form.setQuantity(quantity);
        form.setAddressId(addressId);
        form.setRemark("集成测试下单");
        return form;
    }

    private long paymentCount(Long orderId) {
        return paymentRecordMapper.selectCount(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId));
    }

    private PaymentRecord paymentRecord(Long orderId) {
        PaymentRecord record = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId));
        assertNotNull(record, "支付流水缺失");
        return record;
    }
}
