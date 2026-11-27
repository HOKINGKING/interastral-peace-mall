package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.FeedbackStatus;
import com.ipc.mall.common.constant.FeedbackType;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.entity.Account;
import com.ipc.mall.entity.Feedback;
import com.ipc.mall.entity.Order;
import com.ipc.mall.mapper.AccountMapper;
import com.ipc.mall.mapper.FeedbackMapper;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.service.AdminFeedbackService;
import com.ipc.mall.vo.AdminFeedbackRowVO;
import com.ipc.mall.vo.FeedbackDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 后台投诉与反馈实现（PRD §6.6 / §8.4）。
 * <p>
 * 受理 / 回复 / 关闭全部走条件更新：影响 0 行说明状态已被别人改过，统一返回 3002。
 */
@Slf4j
@Service
public class AdminFeedbackServiceImpl implements AdminFeedbackService {

    private static final int REPLY_MAX = 500;
    private static final int CLOSE_REASON_MAX = 100;

    private final FeedbackMapper feedbackMapper;
    private final OrderMapper orderMapper;
    private final AccountMapper accountMapper;

    public AdminFeedbackServiceImpl(FeedbackMapper feedbackMapper, OrderMapper orderMapper,
                                    AccountMapper accountMapper) {
        this.feedbackMapper = feedbackMapper;
        this.orderMapper = orderMapper;
        this.accountMapper = accountMapper;
    }

    @Override
    public IPage<AdminFeedbackRowVO> page(String type, String status, String orderNo, String username,
                                          LocalDate dateFrom, LocalDate dateTo, int pageNo, int size) {
        Page<AdminFeedbackRowVO> request = new Page<>(Math.max(1, pageNo), Math.max(1, size));
        // 结束日期按「次日 00:00」开区间，含结束当天全天
        LocalDateTime from = dateFrom == null ? null : dateFrom.atStartOfDay();
        LocalDateTime to = dateTo == null ? null : dateTo.plusDays(1).atStartOfDay();
        return feedbackMapper.selectAdminPage(request, normalizeType(type), normalizeStatus(status),
                trimToNull(orderNo), trimToNull(username), from, to);
    }

    @Override
    public FeedbackDetailVO detail(Long id) {
        Feedback feedback = requireFeedback(id);
        Order order = feedback.getOrderId() == null ? null : orderMapper.selectById(feedback.getOrderId());
        FeedbackDetailVO detail = FeedbackDetailVO.of(feedback, order);
        // 后台详情要展示提交人；前台走 FeedbackServiceImpl，不带账号信息
        Account account = feedback.getAccountId() == null ? null : accountMapper.selectById(feedback.getAccountId());
        if (account != null) {
            detail.setAccountId(account.getId());
            detail.setUsername(account.getUsername());
            detail.setNickname(account.getNickname());
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long id) {
        requireFeedback(id);
        int rows = feedbackMapper.markAccepted(id, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        log.info("后台受理投诉/反馈: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, String reply, Long adminId) {
        requireFeedback(id);
        String text = trimToNull(reply);
        if (text == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "请填写回复内容");
        }
        if (text.length() > REPLY_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(), "回复内容最多 500 个字符");
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = feedbackMapper.markReplied(id, text, adminId, now, now);
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        log.info("后台回复投诉/反馈: id={} adminId={}", id, adminId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id, String closeReason) {
        requireFeedback(id);
        String reason = trimToNull(closeReason);
        if (reason == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "请填写关闭原因");
        }
        if (reason.length() > CLOSE_REASON_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(), "关闭原因最多 100 个字符");
        }
        int rows = feedbackMapper.markClosedByAdmin(id, reason, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        log.info("后台关闭投诉/反馈: id={}", id);
    }

    // ------------------------------------------------------------------ 内部方法

    private Feedback requireFeedback(Long id) {
        Feedback feedback = id == null ? null : feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "投诉或反馈不存在或已被删除");
        }
        return feedback;
    }

    private String normalizeType(String type) {
        String value = trimToNull(type);
        if (value == null) {
            return null;
        }
        return FeedbackType.COMPLAINT.equals(value) || FeedbackType.FEEDBACK.equals(value) ? value : null;
    }

    private String normalizeStatus(String status) {
        String value = trimToNull(status);
        if (value == null) {
            return null;
        }
        for (FeedbackStatus candidate : FeedbackStatus.values()) {
            if (candidate.name().equals(value)) {
                return value;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
