package com.ipc.mall.vo;

import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.entity.Order;
import com.ipc.mall.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单详情（PRD §5.13）：明细、收货快照、金额、状态时间线、物流。
 * <p>
 * 收货信息来自 {@code orders} 的快照列，不要回查 address 表（地址可能已被改或删）。
 */
@Data
public class OrderDetailVO {

    private static final DateTimeFormatter SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;

    private String orderNo;

    private String status;

    private String statusDesc;

    private String statusClass;

    private String createTimeText;

    private List<OrderItemVO> items = new ArrayList<>();

    private String receiverName;

    private String receiverPhoneMasked;

    private String receiverAddress;

    private String remark;

    private BigDecimal goodsAmount;

    private BigDecimal freightAmount;

    private BigDecimal totalAmount;

    private String goodsAmountText;

    private String freightAmountText;

    private String totalAmountText;

    private String courierCompany;

    private String trackingNo;

    private List<TimelineNode> timeline = new ArrayList<>();

    private boolean canPay;

    private boolean canCancel;

    private boolean canReceive;

    private boolean canDelete;

    private boolean canComplaint;

    public static OrderDetailVO of(Order order, List<OrderItem> lines) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatus.descOf(order.getStatus()));
        vo.setStatusClass(OrderSummaryVO.badgeClassOf(order.getStatus()));
        vo.setCreateTimeText(format(order.getCreateTime()));
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhoneMasked(AccountProfileVO.maskPhone(order.getReceiverPhone()));
        vo.setReceiverAddress(joinAddress(order));
        vo.setRemark(order.getRemark());
        vo.setCourierCompany(order.getCourierCompany());
        vo.setTrackingNo(order.getTrackingNo());

        BigDecimal goods = BigDecimal.ZERO;
        for (OrderItem line : lines) {
            OrderItemVO item = OrderItemVO.of(line);
            vo.getItems().add(item);
            goods = goods.add(item.getSubtotal());
        }
        goods = OrderItemVO.scale(goods);
        vo.setGoodsAmount(goods);
        vo.setFreightAmount(OrderItemVO.scale(order.getFreightAmount()));
        vo.setTotalAmount(OrderItemVO.scale(order.getTotalAmount()));
        vo.setGoodsAmountText(goods.toPlainString());
        vo.setFreightAmountText(vo.getFreightAmount().toPlainString());
        vo.setTotalAmountText(vo.getTotalAmount().toPlainString());

        vo.setCanPay(OrderStatus.PENDING_PAYMENT.name().equals(order.getStatus()));
        vo.setCanCancel(vo.isCanPay());
        vo.setCanReceive(OrderStatus.SHIPPED.name().equals(order.getStatus()));
        vo.setCanDelete(OrderStatus.COMPLETED.name().equals(order.getStatus())
                || OrderStatus.CANCELLED.name().equals(order.getStatus()));
        vo.setCanComplaint(OrderStatus.PAID.name().equals(order.getStatus())
                || OrderStatus.SHIPPED.name().equals(order.getStatus())
                || OrderStatus.COMPLETED.name().equals(order.getStatus()));

        vo.setTimeline(timelineOf(order));
        return vo;
    }

    /**
     * 状态时间线：下单 → 支付 → 发货 → 完成；取消单在下单后直接给一条「已取消」。
     * 未发生的节点不展示时间，只灰着（页面用 done 区分）。
     */
    private static List<TimelineNode> timelineOf(Order order) {
        List<TimelineNode> nodes = new ArrayList<>();
        nodes.add(TimelineNode.of("提交订单", order.getCreateTime(), true));

        if (OrderStatus.CANCELLED.name().equals(order.getStatus())) {
            nodes.add(TimelineNode.of("订单已取消，库存已回补", order.getCancelTime(), true));
            return nodes;
        }
        nodes.add(TimelineNode.of("支付成功", order.getPayTime(), order.getPayTime() != null));
        String shipLabel = "卖家已发货";
        if (order.getCourierCompany() != null || order.getTrackingNo() != null) {
            shipLabel = "卖家已发货 · " + nullToEmpty(order.getCourierCompany())
                    + " " + nullToEmpty(order.getTrackingNo());
        }
        nodes.add(TimelineNode.of(shipLabel.trim(), order.getShipTime(), order.getShipTime() != null));
        nodes.add(TimelineNode.of("确认收货，交易完成", order.getFinishTime(), order.getFinishTime() != null));
        return nodes;
    }

    private static String joinAddress(Order order) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, order.getReceiverProvince());
        appendPart(builder, order.getReceiverCity());
        appendPart(builder, order.getReceiverDistrict());
        appendPart(builder, order.getReceiverDetail());
        return builder.toString().trim();
    }

    private static void appendPart(StringBuilder builder, String part) {
        if (part != null && !part.isBlank()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.trim());
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : SECOND.format(time);
    }

    /** 时间线节点 */
    @Data
    public static class TimelineNode {

        private String label;

        private String timeText;

        private boolean done;

        public static TimelineNode of(String label, LocalDateTime time, boolean done) {
            TimelineNode node = new TimelineNode();
            node.setLabel(label);
            node.setTimeText(format(time));
            node.setDone(done);
            return node;
        }
    }
}
