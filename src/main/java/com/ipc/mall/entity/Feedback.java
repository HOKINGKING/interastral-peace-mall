package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ipc.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 投诉与反馈（PRD §7.3 feedback / §8.4 状态机）。
 * <p>
 * 投诉与反馈共用本表，用 {@code type} 区分：
 * 投诉（COMPLAINT）必须绑 orderId，且只能绑本人 PAID/SHIPPED/COMPLETED 的订单；
 * 反馈（FEEDBACK）不绑订单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("feedback")
public class Feedback extends BaseEntity {

    /** COMPLAINT 投诉 / FEEDBACK 反馈，见 FeedbackType */
    private String type;

    private Long accountId;

    /** 投诉必填，反馈为 null */
    private Long orderId;

    /** QUALITY / LOGISTICS / SERVICE / OTHER */
    private String category;

    /** 正文，纯文本，最 500 字 */
    private String content;

    /** 【P1】逗号分隔的图片 URL，最多 3 张 */
    private String images;

    /** 联系方式，选填 */
    private String contact;

    /** PENDING / PROCESSING / REPLIED / CLOSED，见 FeedbackStatus */
    private String status;

    /** 卖家回复 */
    private String reply;

    /** 回复的管理员 id */
    private Long replyAdminId;

    private LocalDateTime replyTime;

    /** 用户撤销 / 卖家关闭的原因 */
    private String closeReason;

    /** 是否投诉（绑订单） */
    public boolean isComplaint() {
        return "COMPLAINT".equals(type);
    }
}
