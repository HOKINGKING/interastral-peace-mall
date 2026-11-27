package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.FeedbackCategory;
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
import com.ipc.mall.service.FeedbackService;
import com.ipc.mall.vo.FeedbackDetailVO;
import com.ipc.mall.vo.FeedbackFormVO;
import com.ipc.mall.vo.FeedbackRowVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 前台投诉与反馈实现（PRD §5.15 / §8.4）。
 * <p>
 * 提交规则按类型分叉：
 * <ul>
 *     <li><b>投诉</b>：必须绑本人 PAID/SHIPPED/COMPLETED 订单（3007），同订单只能有一条未关闭投诉（3008），分类必填；</li>
 *     <li><b>反馈</b>：不绑订单，同一用户 1 小时最多 5 条（3009），分类选填。</li>
 * </ul>
 * 状态流转全部走条件更新，0 行即 3002。
 */
@Slf4j
@Service
public class FeedbackServiceImpl implements FeedbackService {

    private static final int CONTENT_MIN_COMPLAINT = 10;
    private static final int CONTENT_MIN_FEEDBACK = 5;
    private static final int CONTENT_MAX = 500;
    private static final int CONTACT_MAX = 50;
    private static final int NOTE_MAX = 200;
    private static final int NOTE_TOTAL_MAX = 1000;
    private static final int IMAGE_MAX = 3;
    private static final int HOURLY_LIMIT = 5;

    private static final Set<String> COMPLAINT_ORDER_STATUS = Set.of(
            OrderStatus.PAID.name(), OrderStatus.SHIPPED.name(), OrderStatus.COMPLETED.name());
    private static final DateTimeFormatter NOTE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FeedbackMapper feedbackMapper;
    private final OrderMapper orderMapper;

    public FeedbackServiceImpl(FeedbackMapper feedbackMapper, OrderMapper orderMapper) {
        this.feedbackMapper = feedbackMapper;
        this.orderMapper = orderMapper;
    }

    // ------------------------------------------------------------------ 列表 / 详情

    @Override
    public IPage<FeedbackRowVO> pageMine(Long accountId, String type, String status, int pageNo, int size) {
        Page<Feedback> request = new Page<>(Math.max(1, pageNo), Math.max(1, size));
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getAccountId, accountId);
        String typeFilter = normalizeType(type);
        if (typeFilter != null) {
            wrapper.eq(Feedback::getType, typeFilter);
        }
        String statusFilter = normalizeStatus(status);
        if (statusFilter != null) {
            wrapper.eq(Feedback::getStatus, statusFilter);
        }
        wrapper.orderByDesc(Feedback::getCreateTime).orderByDesc(Feedback::getId);

        IPage<Feedback> raw = feedbackMapper.selectPage(request, wrapper);
        Map<Long, Order> orders = ordersOf(raw.getRecords());

        Page<FeedbackRowVO> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(raw.getRecords().stream()
                .map(feedback -> FeedbackRowVO.of(feedback, orders.get(feedback.getOrderId())))
                .toList());
        return result;
    }

    @Override
    public FeedbackDetailVO detail(Long accountId, Long id) {
        Feedback feedback = findOwned(accountId, id);
        Order order = feedback.getOrderId() == null ? null : orderMapper.selectById(feedback.getOrderId());
        return FeedbackDetailVO.of(feedback, order);
    }

    @Override
    public FeedbackFormVO formContext(Long accountId, Long orderId) {
        if (orderId == null) {
            return FeedbackFormVO.feedback();
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getAccountId(), accountId)
                || !COMPLAINT_ORDER_STATUS.contains(order.getStatus())) {
            throw new BizException(ResultCode.FEEDBACK_ORDER_INVALID.getCode(),
                    "只能投诉本人已支付/已发货/已完成的订单");
        }
        boolean duplicated = feedbackMapper.countOpenComplaint(order.getId(), accountId) > 0;
        String blocked = duplicated ? "该订单已有未关闭的投诉，可在「我的投诉与反馈」中查看" : null;
        return FeedbackFormVO.complaint(order, blocked);
    }

    // ------------------------------------------------------------------ 提交

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long accountId, FeedbackForm form) {
        if (accountId == null || form == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "提交参数缺失");
        }
        boolean complaint = form.getOrderId() != null;
        String content = trimToNull(form.getContent());
        int minLength = complaint ? CONTENT_MIN_COMPLAINT : CONTENT_MIN_FEEDBACK;
        if (content == null || content.length() < minLength || content.length() > CONTENT_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(),
                    complaint ? "投诉内容需为 10–500 字" : "反馈内容需为 5–500 字");
        }
        String contact = trimToNull(form.getContact());
        if (contact != null && contact.length() > CONTACT_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(), "联系方式最多 50 个字符");
        }
        String category = trimToNull(form.getCategory());
        if (category != null && !FeedbackCategory.isValid(category)) {
            throw new BizException(ResultCode.FAIL.getCode(), "分类参数非法");
        }

        if (complaint) {
            if (category == null) {
                throw new BizException(ResultCode.FAIL.getCode(), "请选择投诉分类");
            }
            Order order = orderMapper.selectById(form.getOrderId());
            if (order == null || !Objects.equals(order.getAccountId(), accountId)
                    || !COMPLAINT_ORDER_STATUS.contains(order.getStatus())) {
                throw new BizException(ResultCode.FEEDBACK_ORDER_INVALID);
            }
            if (feedbackMapper.countOpenComplaint(order.getId(), accountId) > 0) {
                throw new BizException(ResultCode.FEEDBACK_DUPLICATE);
            }
        } else {
            long recent = feedbackMapper.countRecentFeedback(accountId, LocalDateTime.now().minusHours(1));
            if (recent >= HOURLY_LIMIT) {
                throw new BizException(ResultCode.FEEDBACK_RATE_LIMIT);
            }
        }

        Feedback feedback = new Feedback();
        feedback.setType(complaint ? FeedbackType.COMPLAINT : FeedbackType.FEEDBACK);
        feedback.setAccountId(accountId);
        feedback.setOrderId(complaint ? form.getOrderId() : null);
        feedback.setCategory(category);
        feedback.setContent(content);
        feedback.setContact(contact);
        feedback.setImages(normalizeImages(form.getImages()));
        feedback.setStatus(FeedbackStatus.PENDING.name());
        feedbackMapper.insert(feedback);
        log.info("提交{}: id={} account={} orderId={}", FeedbackType.descOf(feedback.getType()),
                feedback.getId(), accountId, feedback.getOrderId());
    }

    // ------------------------------------------------------------------ 状态流转

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long accountId, Long id) {
        findOwned(accountId, id);
        int rows = feedbackMapper.cancelByUser(id, accountId, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        log.info("用户撤销投诉/反馈: id={} account={}", id, accountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addNote(Long accountId, Long id, String note) {
        Feedback feedback = findOwned(accountId, id);
        if (FeedbackStatus.CLOSED.name().equals(feedback.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        String text = trimToNull(note);
        if (text == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "请填写补充说明");
        }
        if (text.length() > NOTE_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(), "补充说明最多 200 个字符");
        }
        String line = "【" + LocalDateTime.now().format(NOTE_TIME) + "】" + text;
        String merged = feedback.getUserNote() == null || feedback.getUserNote().isBlank()
                ? line : feedback.getUserNote() + "\n" + line;
        if (merged.length() > NOTE_TOTAL_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(), "补充说明总长不能超过 1000 字");
        }
        int rows = feedbackMapper.updateUserNote(id, accountId, merged, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolve(Long accountId, Long id) {
        Feedback feedback = findOwned(accountId, id);
        if (!FeedbackStatus.REPLIED.name().equals(feedback.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        int rows = feedbackMapper.resolveByUser(id, accountId, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        log.info("用户确认已解决: id={} account={}", id, accountId);
    }

    // ------------------------------------------------------------------ 内部方法

    private Feedback findOwned(Long accountId, Long id) {
        Feedback feedback = id == null ? null : feedbackMapper.selectById(id);
        if (feedback == null || !Objects.equals(feedback.getAccountId(), accountId)) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "投诉或反馈不存在");
        }
        return feedback;
    }

    /** 一页只查一次订单，避免每行一次查询 */
    private Map<Long, Order> ordersOf(List<Feedback> feedbacks) {
        Map<Long, Order> orders = new HashMap<>();
        List<Long> orderIds = feedbacks.stream()
                .map(Feedback::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!orderIds.isEmpty()) {
            for (Order order : orderMapper.selectBatchIds(orderIds)) {
                orders.put(order.getId(), order);
            }
        }
        return orders;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String value = type.trim();
        return FeedbackType.COMPLAINT.equals(value) || FeedbackType.FEEDBACK.equals(value) ? value : null;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim();
        for (FeedbackStatus candidate : FeedbackStatus.values()) {
            if (candidate.name().equals(value)) {
                return value;
            }
        }
        return null;
    }

    private String normalizeImages(String images) {
        String value = trimToNull(images);
        if (value == null) {
            return null;
        }
        List<String> urls = java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .toList();
        if (urls.size() > IMAGE_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(), "最多上传 3 张图片");
        }
        return urls.isEmpty() ? null : String.join(",", urls);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
