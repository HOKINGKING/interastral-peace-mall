package com.ipc.mall.vo;

import com.ipc.mall.common.constant.FeedbackStatus;
import com.ipc.mall.common.constant.FeedbackType;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 后台投诉/反馈列表行（PRD §6.6 / docs/api.md A28）。
 * <p>
 * 由 {@code FeedbackMapper.selectAdminPage} 直接映射（JOIN account / orders），
 * 状态文案与颜色在 getter 里现算，避免 SQL 里写死展示逻辑。
 */
@Data
public class AdminFeedbackRowVO {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PREVIEW_LENGTH = 30;

    private Long id;

    private String type;

    private Long accountId;

    private String username;

    private String nickname;

    private Long orderId;

    private String orderNo;

    private String content;

    private String status;

    private LocalDateTime createTime;

    /** 1=已回复，0=未回复（SQL 里 CASE 出来） */
    private Boolean replied;

    public String getTypeDesc() {
        return FeedbackType.descOf(type);
    }

    public String getStatusDesc() {
        return FeedbackStatus.descOf(status);
    }

    public String getStatusClass() {
        return FeedbackStatus.badgeClassOf(status);
    }

    public String getDisplayName() {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }

    public String getContentPreview() {
        if (content == null || content.isBlank()) {
            return "";
        }
        String text = content.replaceAll("\\s+", " ").trim();
        return text.length() <= PREVIEW_LENGTH ? text : text.substring(0, PREVIEW_LENGTH) + "…";
    }

    public String getCreateTimeText() {
        return createTime == null ? "" : MINUTE.format(createTime);
    }
}
