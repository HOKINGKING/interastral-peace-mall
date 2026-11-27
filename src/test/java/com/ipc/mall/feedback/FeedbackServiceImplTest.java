package com.ipc.mall.feedback;

import com.ipc.mall.common.constant.FeedbackStatus;
import com.ipc.mall.common.constant.FeedbackType;
import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.FeedbackForm;
import com.ipc.mall.entity.Feedback;
import com.ipc.mall.entity.Order;
import com.ipc.mall.mapper.FeedbackMapper;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.service.impl.FeedbackServiceImpl;
import com.ipc.mall.vo.FeedbackDetailVO;
import com.ipc.mall.vo.FeedbackFormVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 前台投诉与反馈（PRD §5.15 / §8.4）：提交规则、状态流转、归属校验。
 */
class FeedbackServiceImplTest {

    private FeedbackMapper feedbackMapper;
    private OrderMapper orderMapper;
    private FeedbackServiceImpl service;

    @BeforeEach
    void setUp() {
        feedbackMapper = mock(FeedbackMapper.class);
        orderMapper = mock(OrderMapper.class);
        service = new FeedbackServiceImpl(feedbackMapper, orderMapper);
    }

    // ------------------------------------------------------------ 投诉提交

    @Test
    void 投诉本人已支付订单成功() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 1L, OrderStatus.PAID));
        when(feedbackMapper.countOpenComplaint(5L, 1L)).thenReturn(0L);

        service.submit(1L, complaint(5L, "QUALITY", "商品破损了一角，希望处理一下"));

        Feedback saved = capturedInsert();
        assertEquals(FeedbackType.COMPLAINT, saved.getType());
        assertEquals(FeedbackStatus.PENDING.name(), saved.getStatus());
        assertEquals(1L, saved.getAccountId());
        assertEquals(5L, saved.getOrderId());
        assertEquals("QUALITY", saved.getCategory());
        assertEquals("商品破损了一角，希望处理一下", saved.getContent());
    }

    @Test
    void 投诉他人订单返回三千零七() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 2L, OrderStatus.PAID));

        BizException exception = assertThrows(BizException.class,
                () -> service.submit(1L, complaint(5L, "QUALITY", "商品破损了一角，希望处理一下")));

        assertEquals(ResultCode.FEEDBACK_ORDER_INVALID.getCode(), exception.getCode());
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    @Test
    void 投诉未支付订单返回三千零七() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 1L, OrderStatus.PENDING_PAYMENT));

        BizException exception = assertThrows(BizException.class,
                () -> service.submit(1L, complaint(5L, "QUALITY", "商品破损了一角，希望处理一下")));

        assertEquals(ResultCode.FEEDBACK_ORDER_INVALID.getCode(), exception.getCode());
    }

    @Test
    void 投诉必须选分类() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 1L, OrderStatus.PAID));

        BizException exception = assertThrows(BizException.class,
                () -> service.submit(1L, complaint(5L, null, "商品破损了一角，希望处理一下")));

        assertTrue(exception.getMessage().contains("分类"));
    }

    @Test
    void 投诉内容少于十字返回四百() {
        BizException exception = assertThrows(BizException.class,
                () -> service.submit(1L, complaint(5L, "QUALITY", "太差了")));

        assertTrue(exception.getMessage().contains("10"));
        verifyNoInsert();
    }

    @Test
    void 同订单重复投诉返回三千零八() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 1L, OrderStatus.SHIPPED));
        when(feedbackMapper.countOpenComplaint(5L, 1L)).thenReturn(1L);

        BizException exception = assertThrows(BizException.class,
                () -> service.submit(1L, complaint(5L, "LOGISTICS", "物流太慢，等了整整一周还没到")));

        assertEquals(ResultCode.FEEDBACK_DUPLICATE.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 反馈提交

    @Test
    void 反馈不绑订单且分类可空() {
        when(feedbackMapper.countRecentFeedback(eq(1L), any(LocalDateTime.class))).thenReturn(0L);

        service.submit(1L, feedback("界面很好看，希望多上架一些新商品"));

        Feedback saved = capturedInsert();
        assertEquals(FeedbackType.FEEDBACK, saved.getType());
        assertNull(saved.getOrderId());
        assertNull(saved.getCategory());
        assertEquals(FeedbackStatus.PENDING.name(), saved.getStatus());
    }

    @Test
    void 一小时内第五条之后的反馈返回三千零九() {
        when(feedbackMapper.countRecentFeedback(eq(1L), any(LocalDateTime.class))).thenReturn(5L);

        BizException exception = assertThrows(BizException.class,
                () -> service.submit(1L, feedback("界面很好看，希望多上架一些新商品")));

        assertEquals(ResultCode.FEEDBACK_RATE_LIMIT.getCode(), exception.getCode());
        verifyNoInsert();
    }

    @Test
    void 反馈内容少于五字返回四百() {
        BizException exception = assertThrows(BizException.class, () -> service.submit(1L, feedback("还行")));

        assertTrue(exception.getMessage().contains("5"));
    }

    // ------------------------------------------------------------ 状态流转

    @Test
    void 撤销仅待处理可成功() {
        Feedback feedback = feedback(9L, FeedbackType.COMPLAINT, FeedbackStatus.PENDING, 1L, 5L);
        when(feedbackMapper.selectById(9L)).thenReturn(feedback);
        when(feedbackMapper.cancelByUser(eq(9L), eq(1L), any(LocalDateTime.class))).thenReturn(1);

        service.cancel(1L, 9L);

        verify(feedbackMapper).cancelByUser(eq(9L), eq(1L), any(LocalDateTime.class));
    }

    @Test
    void 撤销已处理反馈返回三千零二() {
        Feedback feedback = feedback(9L, FeedbackType.COMPLAINT, FeedbackStatus.PROCESSING, 1L, 5L);
        when(feedbackMapper.selectById(9L)).thenReturn(feedback);
        when(feedbackMapper.cancelByUser(eq(9L), eq(1L), any(LocalDateTime.class))).thenReturn(0);

        BizException exception = assertThrows(BizException.class, () -> service.cancel(1L, 9L));

        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
    }

    @Test
    void 补充说明按时间前缀追加() {
        Feedback feedback = feedback(9L, FeedbackType.COMPLAINT, FeedbackStatus.PROCESSING, 1L, 5L);
        feedback.setUserNote("【2026-11-01 10:00】旧说明");
        when(feedbackMapper.selectById(9L)).thenReturn(feedback);
        when(feedbackMapper.updateUserNote(eq(9L), eq(1L), anyString(), any(LocalDateTime.class))).thenReturn(1);

        service.addNote(1L, 9L, "补充一句");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(feedbackMapper).updateUserNote(eq(9L), eq(1L), captor.capture(), any(LocalDateTime.class));
        String merged = captor.getValue();
        assertTrue(merged.startsWith("【2026-11-01 10:00】旧说明\n【"));
        assertTrue(merged.endsWith("】补充一句"));
    }

    @Test
    void 已关闭不能补充说明() {
        Feedback feedback = feedback(9L, FeedbackType.FEEDBACK, FeedbackStatus.CLOSED, 1L, null);
        when(feedbackMapper.selectById(9L)).thenReturn(feedback);

        BizException exception = assertThrows(BizException.class, () -> service.addNote(1L, 9L, "补充"));

        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
        verify(feedbackMapper, never()).updateUserNote(any(), any(), any(), any());
    }

    @Test
    void 补充说明总长超过一千字返回四百() {
        Feedback feedback = feedback(9L, FeedbackType.COMPLAINT, FeedbackStatus.PENDING, 1L, 5L);
        feedback.setUserNote("x".repeat(950));
        when(feedbackMapper.selectById(9L)).thenReturn(feedback);

        BizException exception = assertThrows(BizException.class,
                () -> service.addNote(1L, 9L, "y".repeat(100)));

        assertTrue(exception.getMessage().contains("1000"));
    }

    @Test
    void 确认解决仅已回复状态可用() {
        Feedback feedback = feedback(9L, FeedbackType.COMPLAINT, FeedbackStatus.REPLIED, 1L, 5L);
        when(feedbackMapper.selectById(9L)).thenReturn(feedback);
        when(feedbackMapper.resolveByUser(eq(9L), eq(1L), any(LocalDateTime.class))).thenReturn(1);

        service.resolve(1L, 9L);

        verify(feedbackMapper).resolveByUser(eq(9L), eq(1L), any(LocalDateTime.class));
    }

    @Test
    void 处理中不能直接确认解决() {
        Feedback feedback = feedback(9L, FeedbackType.COMPLAINT, FeedbackStatus.PROCESSING, 1L, 5L);
        when(feedbackMapper.selectById(9L)).thenReturn(feedback);

        BizException exception = assertThrows(BizException.class, () -> service.resolve(1L, 9L));

        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
        verify(feedbackMapper, never()).resolveByUser(any(), any(), any());
    }

    @Test
    void 查看他人反馈按不存在处理() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, FeedbackType.FEEDBACK,
                FeedbackStatus.PENDING, 2L, null));

        BizException exception = assertThrows(BizException.class, () -> service.detail(1L, 9L));

        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void 详情带出订单摘要与操作标记() {
        Feedback feedback = feedback(9L, FeedbackType.COMPLAINT, FeedbackStatus.REPLIED, 1L, 5L);
        feedback.setReply("已补发");
        feedback.setReplyTime(LocalDateTime.of(2026, 11, 27, 14, 0));
        when(feedbackMapper.selectById(9L)).thenReturn(feedback);
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 1L, OrderStatus.COMPLETED));

        FeedbackDetailVO detail = service.detail(1L, 9L);

        assertEquals("投诉", detail.getTypeDesc());
        assertEquals("已回复", detail.getStatusDesc());
        assertNotNull(detail.getOrder());
        assertEquals("20261116000005", detail.getOrder().getOrderNo());
        assertTrue(detail.isCanResolve());
        assertTrue(detail.isCanNote());
        assertFalse(detail.isCanCancel());
    }

    // ------------------------------------------------------------ 表单上下文

    @Test
    void 已有未关闭投诉时表单标记阻塞() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 1L, OrderStatus.PAID));
        when(feedbackMapper.countOpenComplaint(5L, 1L)).thenReturn(1L);

        FeedbackFormVO context = service.formContext(1L, 5L);

        assertTrue(context.isComplaint());
        assertNotNull(context.getBlocked());
        assertEquals("20261116000005", context.getOrderNo());
    }

    @Test
    void 投诉他人订单的表单上下文返回三千零七() {
        when(orderMapper.selectById(5L)).thenReturn(order(5L, 2L, OrderStatus.PAID));

        BizException exception = assertThrows(BizException.class, () -> service.formContext(1L, 5L));

        assertEquals(ResultCode.FEEDBACK_ORDER_INVALID.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 夹具

    private void verifyNoInsert() {
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }

    private Feedback capturedInsert() {
        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackMapper).insert(captor.capture());
        return captor.getValue();
    }

    private FeedbackForm complaint(Long orderId, String category, String content) {
        FeedbackForm form = new FeedbackForm();
        form.setOrderId(orderId);
        form.setCategory(category);
        form.setContent(content);
        return form;
    }

    private FeedbackForm feedback(String content) {
        FeedbackForm form = new FeedbackForm();
        form.setContent(content);
        return form;
    }

    private Feedback feedback(Long id, String type, FeedbackStatus status, Long accountId, Long orderId) {
        Feedback feedback = new Feedback();
        feedback.setId(id);
        feedback.setType(type);
        feedback.setStatus(status.name());
        feedback.setAccountId(accountId);
        feedback.setOrderId(orderId);
        feedback.setContent("内容");
        feedback.setCreateTime(LocalDateTime.of(2026, 11, 27, 13, 0));
        return feedback;
    }

    private Order order(Long id, Long accountId, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setAccountId(accountId);
        order.setStatus(status.name());
        order.setOrderNo("2026111600000" + id);
        order.setTotalAmount(new BigDecimal("99.00"));
        order.setCreateTime(LocalDateTime.of(2026, 11, 16, 10, 21));
        return order;
    }
}
