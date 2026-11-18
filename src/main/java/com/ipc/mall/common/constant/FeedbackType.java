package com.ipc.mall.common.constant;

/**
 * 投诉/反馈类型（feedback.type，PRD §5.15）。
 * 两者共用一张表与一套状态机，用 type 区分：投诉必须绑订单，反馈不绑。
 */
public final class FeedbackType {

    public static final String COMPLAINT = "COMPLAINT";
    public static final String FEEDBACK = "FEEDBACK";

    private FeedbackType() {
    }

    public static boolean isComplaint(String type) {
        return COMPLAINT.equals(type);
    }

    public static String descOf(String type) {
        return COMPLAINT.equals(type) ? "投诉" : FEEDBACK.equals(type) ? "反馈" : type;
    }
}
