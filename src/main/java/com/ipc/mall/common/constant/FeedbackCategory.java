package com.ipc.mall.common.constant;

/**
 * 投诉/反馈的分类（feedback.category，PRD §5.15）。
 * <p>
 * 投诉必填、反馈选填；四个枚举值由数据库 CHECK 约束兜底。
 */
public final class FeedbackCategory {

    public static final String QUALITY = "QUALITY";
    public static final String LOGISTICS = "LOGISTICS";
    public static final String SERVICE = "SERVICE";
    public static final String OTHER = "OTHER";

    private FeedbackCategory() {
    }

    public static boolean isValid(String value) {
        return QUALITY.equals(value) || LOGISTICS.equals(value)
                || SERVICE.equals(value) || OTHER.equals(value);
    }

    public static String descOf(String value) {
        if (QUALITY.equals(value)) {
            return "商品质量";
        }
        if (LOGISTICS.equals(value)) {
            return "物流发货";
        }
        if (SERVICE.equals(value)) {
            return "服务态度";
        }
        if (OTHER.equals(value)) {
            return "其他";
        }
        return value;
    }
}
