package com.ipc.mall.vo;

import com.ipc.mall.common.constant.FeedbackStatus;
import com.ipc.mall.common.constant.FeedbackType;
import com.ipc.mall.entity.Feedback;
import com.ipc.mall.entity.Order;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 前台「我的投诉与反馈」列表行（PRD §5.15 / docs/api.md F34）。
 */
@Data
public class FeedbackRowVO {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PREVIEW_LENGTH = 30;

    private Long id;

    private String type;

    private String typeDesc;

    private String status;

    private String statusDesc;

    private String statusClass;

    private Long orderId;

    private String orderNo;

    /** 内容摘要（前 30 字） */
    private String contentPreview;

    private LocalDateTime createTime;

    /** 卖家是否已回复 */
    private Boolean replied;

    public static FeedbackRowVO of(Feedback feedback, Order order) {
        FeedbackRowVO vo = new FeedbackRowVO();
        vo.setId(feedback.getId());
        vo.setType(feedback.getType());
        vo.setTypeDesc(FeedbackType.descOf(feedback.getType()));
        vo.setStatus(feedback.getStatus());
        vo.setStatusDesc(FeedbackStatus.descOf(feedback.getStatus()));
        vo.setStatusClass(FeedbackStatus.badgeClassOf(feedback.getStatus()));
        vo.setOrderId(feedback.getOrderId());
        vo.setOrderNo(order == null ? null : order.getOrderNo());
        vo.setContentPreview(preview(feedback.getContent()));
        vo.setCreateTime(feedback.getCreateTime());
        vo.setReplied(feedback.getReply() != null);
        return vo;
    }

    public String getCreateTimeText() {
        return createTime == null ? "" : MINUTE.format(createTime);
    }

    private static String preview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String text = content.replaceAll("\\s+", " ").trim();
        return text.length() <= PREVIEW_LENGTH ? text : text.substring(0, PREVIEW_LENGTH) + "…";
    }
}
