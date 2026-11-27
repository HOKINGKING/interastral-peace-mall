package com.ipc.mall.feedback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.FeedbackStatus;
import com.ipc.mall.common.constant.FeedbackType;
import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.entity.Account;
import com.ipc.mall.entity.Feedback;
import com.ipc.mall.entity.Order;
import com.ipc.mall.mapper.AccountMapper;
import com.ipc.mall.mapper.FeedbackMapper;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.service.impl.AdminFeedbackServiceImpl;
import com.ipc.mall.vo.FeedbackDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 后台投诉与反馈（PRD §6.6 / §8.4）：筛选、详情、受理/回复/关闭的状态约束。
 */
class AdminFeedbackServiceImplTest {

    private FeedbackMapper feedbackMapper;
    private OrderMapper orderMapper;
    private AccountMapper accountMapper;
    private AdminFeedbackServiceImpl service;

    @BeforeEach
    void setUp() {
        feedbackMapper = mock(FeedbackMapper.class);
        orderMapper = mock(OrderMapper.class);
        accountMapper = mock(AccountMapper.class);
        service = new AdminFeedbackServiceImpl(feedbackMapper, orderMapper, accountMapper);
    }

    // ------------------------------------------------------------ 列表

    @Test
    void 列表把日期转成半开区间且忽略非法筛选值() {
        when(feedbackMapper.selectAdminPage(any(), isNull(), isNull(), isNull(), isNull(),
                eq(LocalDateTime.of(2026, 11, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 11, 28, 0, 0))))
                .thenReturn(new Page<>());

        service.page("NOT_A_TYPE", "NOT_A_STATUS", " ", " ",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 27), 1, 10);

        verify(feedbackMapper).selectAdminPage(any(), isNull(), isNull(), isNull(), isNull(),
                eq(LocalDateTime.of(2026, 11, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 11, 28, 0, 0)));
    }

    // ------------------------------------------------------------ 详情

    @Test
    void 详情带出提交人与订单摘要() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, FeedbackStatus.PENDING));
        when(orderMapper.selectById(5L)).thenReturn(order(5L));
        Account account = new Account();
        account.setId(1L);
        account.setUsername("xingye");
        account.setNickname("星野");
        when(accountMapper.selectById(1L)).thenReturn(account);

        FeedbackDetailVO detail = service.detail(9L);

        assertEquals("xingye", detail.getUsername());
        assertEquals("星野", detail.getDisplayName());
        assertNotNull(detail.getOrder());
        assertEquals("20261116000005", detail.getOrder().getOrderNo());
        assertTrue(detail.isCanAccept());
        assertTrue(detail.isCanReply());
        assertTrue(detail.isCanClose());
    }

    @Test
    void 详情不存在返回四百零四() {
        when(feedbackMapper.selectById(9L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> service.detail(9L));

        assertEquals(ResultCode.NOT_FOUND.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 受理

    @Test
    void 受理成功() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, FeedbackStatus.PENDING));
        when(feedbackMapper.markAccepted(eq(9L), any(LocalDateTime.class))).thenReturn(1);

        service.accept(9L);

        verify(feedbackMapper).markAccepted(eq(9L), any(LocalDateTime.class));
    }

    @Test
    void 重复受理返回三千零二() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, FeedbackStatus.PROCESSING));
        when(feedbackMapper.markAccepted(eq(9L), any(LocalDateTime.class))).thenReturn(0);

        BizException exception = assertThrows(BizException.class, () -> service.accept(9L));

        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 回复

    @Test
    void 回复成功写入回复人与时间() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, FeedbackStatus.PENDING));
        when(feedbackMapper.markReplied(eq(9L), eq("已安排补发"), eq(7L), any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn(1);

        service.reply(9L, "已安排补发", 7L);

        verify(feedbackMapper).markReplied(eq(9L), eq("已安排补发"), eq(7L),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void 回复内容为空返回四百() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, FeedbackStatus.PENDING));

        BizException exception = assertThrows(BizException.class, () -> service.reply(9L, "  ", 7L));

        assertTrue(exception.getMessage().contains("回复"));
        verify(feedbackMapper, never()).markReplied(any(), any(), any(), any(), any());
    }

    @Test
    void 回复超过五百字返回四百() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, FeedbackStatus.PENDING));

        BizException exception = assertThrows(BizException.class,
                () -> service.reply(9L, "x".repeat(501), 7L));

        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    void 已关闭状态回复返回三千零二() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, FeedbackStatus.CLOSED));
        when(feedbackMapper.markReplied(eq(9L), any(), eq(7L), any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn(0);

        BizException exception = assertThrows(BizException.class, () -> service.reply(9L, "回复", 7L));

        assertEquals(ResultCode.ORDER_STATUS_INVALID.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 关闭

    @Test
    void 关闭成功写入原因() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, FeedbackStatus.REPLIED));
        when(feedbackMapper.markClosedByAdmin(eq(9L), eq("已协商解决"), any(LocalDateTime.class))).thenReturn(1);

        service.close(9L, "已协商解决");

        verify(feedbackMapper).markClosedByAdmin(eq(9L), eq("已协商解决"), any(LocalDateTime.class));
    }

    @Test
    void 关闭原因必填且不超过一百字() {
        when(feedbackMapper.selectById(9L)).thenReturn(feedback(9L, FeedbackStatus.PENDING));

        BizException blank = assertThrows(BizException.class, () -> service.close(9L, " "));
        assertTrue(blank.getMessage().contains("关闭原因"));

        BizException tooLong = assertThrows(BizException.class, () -> service.close(9L, "x".repeat(101)));
        assertTrue(tooLong.getMessage().contains("100"));
    }

    // ------------------------------------------------------------ 夹具

    private Feedback feedback(Long id, FeedbackStatus status) {
        Feedback feedback = new Feedback();
        feedback.setId(id);
        feedback.setType(FeedbackType.COMPLAINT);
        feedback.setStatus(status.name());
        feedback.setAccountId(1L);
        feedback.setOrderId(5L);
        feedback.setCategory("QUALITY");
        feedback.setContent("商品破损");
        feedback.setCreateTime(LocalDateTime.of(2026, 11, 27, 13, 0));
        return feedback;
    }

    private Order order(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setAccountId(1L);
        order.setOrderNo("2026111600000" + id);
        order.setStatus(OrderStatus.COMPLETED.name());
        order.setTotalAmount(new BigDecimal("99.00"));
        order.setCreateTime(LocalDateTime.of(2026, 11, 16, 10, 21));
        return order;
    }
}
