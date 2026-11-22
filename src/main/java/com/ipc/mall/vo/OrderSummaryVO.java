package com.ipc.mall.vo;

import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.entity.Order;
import com.ipc.mall.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 订单列表卡片（PRD §5.13）。
 * <p>
 * 一页只查一次明细再按 orderId 分组，避免每张卡片回查一次（N+1）。
 */
@Data
public class OrderSummaryVO {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long id;

    private String orderNo;

    private String status;

    private String statusDesc;

    /** 页面 badge 样式：badge-pending / badge-paid / ... */
    private String statusClass;

    private String createTimeText;

    private BigDecimal totalAmount;

    private String totalAmountText;

    /** 该订单商品总件数 */
    private int totalQuantity;

    private String firstProductName;

    private String firstProductImage;

    private String courierCompany;

    private String trackingNo;

    private boolean canPay;

    private boolean canCancel;

    private boolean canReceive;

    private boolean canDelete;

    private boolean canComplaint;

    public static OrderSummaryVO of(Order order, List<OrderItem> items) {
        OrderSummaryVO vo = new OrderSummaryVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatus.descOf(order.getStatus()));
        vo.setStatusClass(badgeClassOf(order.getStatus()));
        vo.setCreateTimeText(format(order.getCreateTime()));
        vo.setTotalAmount(OrderItemVO.scale(order.getTotalAmount()));
        vo.setTotalAmountText(OrderItemVO.text(order.getTotalAmount()));
        vo.setCourierCompany(order.getCourierCompany());
        vo.setTrackingNo(order.getTrackingNo());

        int quantity = 0;
        for (OrderItem item : items) {
            quantity += item.getQuantity() == null ? 0 : item.getQuantity();
        }
        vo.setTotalQuantity(quantity);
        if (!items.isEmpty()) {
            OrderItem first = items.get(0);
            vo.setFirstProductName(first.getProductName());
            vo.setFirstProductImage(first.getImage());
        }

        boolean pending = OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus());
        vo.setCanPay(pending);
        vo.setCanCancel(pending);
        vo.setCanReceive(OrderStatus.SHIPPED.name().equals(order.getStatus()));
        vo.setCanDelete(OrderStatus.COMPLETED.name().equals(order.getStatus())
                || OrderStatus.CANCELLED.name().equals(order.getStatus()));
        vo.setCanComplaint(OrderStatus.PAID.name().equals(order.getStatus())
                || OrderStatus.SHIPPED.name().equals(order.getStatus())
                || OrderStatus.COMPLETED.name().equals(order.getStatus()));
        return vo;
    }

    /** 状态 → style.css 里已有的 badge 类名 */
    public static String badgeClassOf(String status) {
        if (status == null) {
            return "badge";
        }
        return switch (status) {
            case "PENDING_PAYMENT" -> "badge-pending";
            case "PAID" -> "badge-paid";
            case "SHIPPED" -> "badge-shipped";
            case "COMPLETED" -> "badge-completed";
            case "CANCELLED" -> "badge-cancelled";
            default -> "badge";
        };
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : MINUTE.format(time);
    }
}
