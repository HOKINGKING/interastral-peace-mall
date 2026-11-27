package com.ipc.mall.vo;

import com.ipc.mall.common.constant.FeedbackCategory;
import com.ipc.mall.common.constant.FeedbackStatus;
import com.ipc.mall.common.constant.FeedbackType;
import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.entity.Feedback;
import com.ipc.mall.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 投诉/反馈详情（PRD §5.15 详情 / §6.6 详情）。
 * <p>
 * 前台与后台共用：前台看 {@code canCancel/canNote/canResolve}，
 * 后台看 {@code canAccept/canReply/canClose}，两边都从同一份状态推导。
 */
@Data
public class FeedbackDetailVO {

    private static final DateTimeFormatter SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;

    /** 提交人（后台详情展示用；前台为 null） */
    private Long accountId;

    private String username;

    private String nickname;

    private String type;

    private String typeDesc;

    private String status;

    private String statusDesc;

    private String statusClass;

    private String category;

    private String categoryDesc;

    private String content;

    /** 逗号分隔的图片 URL 拆成列表（P1） */
    private List<String> images = List.of();

    private String contact;

    private String createTimeText;

    private String reply;

    private String replyTimeText;

    private String closeReason;

    /** user_note 按行拆分：后台展示时用 #lists.reverse 倒序 */
    private List<String> notes = List.of();

    /** 关联订单摘要，反馈为 null */
    private OrderBrief order;

    private boolean canCancel;

    private boolean canNote;

    private boolean canResolve;

    private boolean canAccept;

    private boolean canReply;

    private boolean canClose;

    public static FeedbackDetailVO of(Feedback feedback, Order order) {
        FeedbackDetailVO vo = new FeedbackDetailVO();
        vo.setId(feedback.getId());
        vo.setType(feedback.getType());
        vo.setTypeDesc(FeedbackType.descOf(feedback.getType()));
        vo.setStatus(feedback.getStatus());
        vo.setStatusDesc(FeedbackStatus.descOf(feedback.getStatus()));
        vo.setStatusClass(FeedbackStatus.badgeClassOf(feedback.getStatus()));
        vo.setCategory(feedback.getCategory());
        vo.setCategoryDesc(FeedbackCategory.descOf(feedback.getCategory()));
        vo.setContent(feedback.getContent());
        vo.setImages(splitImages(feedback.getImages()));
        vo.setContact(feedback.getContact());
        vo.setCreateTimeText(format(feedback.getCreateTime(), "—"));
        vo.setReply(feedback.getReply());
        vo.setReplyTimeText(feedback.getReplyTime() == null ? null : format(feedback.getReplyTime(), null));
        vo.setCloseReason(feedback.getCloseReason());
        vo.setNotes(splitNotes(feedback.getUserNote()));
        vo.setOrder(OrderBrief.of(order));

        String status = feedback.getStatus();
        boolean pending = FeedbackStatus.PENDING.name().equals(status);
        boolean replied = FeedbackStatus.REPLIED.name().equals(status);
        boolean closed = FeedbackStatus.CLOSED.name().equals(status);
        vo.setCanCancel(pending);
        vo.setCanNote(!closed);
        vo.setCanResolve(replied);
        vo.setCanAccept(pending);
        vo.setCanReply(pending || FeedbackStatus.PROCESSING.name().equals(status));
        vo.setCanClose(!closed);
        return vo;
    }

    public boolean isComplaint() {
        return FeedbackType.isComplaint(type);
    }

    /** 后台展示用：补充说明按时间倒序（最新的在最上面） */
    public List<String> getNotesDesc() {
        List<String> reversed = new ArrayList<>(notes);
        java.util.Collections.reverse(reversed);
        return reversed;
    }

    /** 优先昵称，回落用户名 */
    public String getDisplayName() {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }

    private static List<String> splitImages(String images) {
        if (images == null || images.isBlank()) {
            return List.of();
        }
        return Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .toList();
    }

    private static List<String> splitNotes(String userNote) {
        if (userNote == null || userNote.isBlank()) {
            return List.of();
        }
        List<String> notes = new ArrayList<>();
        for (String line : userNote.split("\\R")) {
            if (!line.isBlank()) {
                notes.add(line.trim());
            }
        }
        return notes;
    }

    private static String format(LocalDateTime time, String fallback) {
        return time == null ? fallback : SECOND.format(time);
    }

    /** 关联订单摘要：只暴露买家需要看到的字段 */
    @Data
    public static class OrderBrief {

        private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private Long id;

        private String orderNo;

        private String totalAmountText;

        private String statusDesc;

        private String createTimeText;

        public static OrderBrief of(Order order) {
            if (order == null) {
                return null;
            }
            OrderBrief brief = new OrderBrief();
            brief.setId(order.getId());
            brief.setOrderNo(order.getOrderNo());
            brief.setTotalAmountText(amountText(order.getTotalAmount()));
            brief.setStatusDesc(OrderStatus.descOf(order.getStatus()));
            brief.setCreateTimeText(order.getCreateTime() == null ? "" : MINUTE.format(order.getCreateTime()));
            return brief;
        }

        private static String amountText(BigDecimal amount) {
            return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
    }
}
