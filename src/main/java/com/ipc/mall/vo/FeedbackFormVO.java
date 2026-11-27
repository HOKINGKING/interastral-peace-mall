package com.ipc.mall.vo;

import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 投诉/反馈表单页的上下文（PRD §5.15 / docs/api.md F32）。
 * <p>
 * 反馈模式只有 {@code complaint=false}；投诉模式会带上订单快照，
 * 若该订单已有未关闭投诉，{@code blocked} 非空，页面只提示不允许再提交。
 */
@Data
public class FeedbackFormVO {

    private boolean complaint;

    private Long orderId;

    private String orderNo;

    private String totalAmountText;

    private String orderStatusDesc;

    private String createTimeText;

    /** 已有未关闭投诉等阻塞原因；非空时页面禁用提交 */
    private String blocked;

    public static FeedbackFormVO feedback() {
        FeedbackFormVO vo = new FeedbackFormVO();
        vo.setComplaint(false);
        return vo;
    }

    public static FeedbackFormVO complaint(Order order, String blocked) {
        FeedbackFormVO vo = new FeedbackFormVO();
        vo.setComplaint(true);
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmountText(amountText(order.getTotalAmount()));
        vo.setOrderStatusDesc(OrderStatus.descOf(order.getStatus()));
        vo.setCreateTimeText(order.getCreateTime() == null ? "" : order.getCreateTime().toString());
        vo.setBlocked(blocked);
        return vo;
    }

    private static String amountText(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
