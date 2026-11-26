package com.ipc.mall.vo;

import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.entity.Order;
import com.ipc.mall.entity.OrderItem;
import com.ipc.mall.entity.PaymentRecord;
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

    /** 下单用户（后台详情展示用，前台为 null） */
    private String username;

    private String status;

    private String statusDesc;

    private String statusClass;

    private String createTimeText;

    private List<OrderItemVO> items = new ArrayList<>();

    private String receiverName;

    private String receiverPhoneMasked;

    /** 完整手机号：仅后台详情页的「修改收货信息」表单使用，前台为 null */
    private String receiverPhoneRaw;

    private String receiverAddress;

    /** 省市区与详细地址分开存：后台修改收货信息时只需要回填详细地址 */
    private String receiverProvince;

    private String receiverCity;

    private String receiverDistrict;

    private String receiverDetail;

    private String remark;

    private BigDecimal goodsAmount;

    private BigDecimal freightAmount;

    private BigDecimal totalAmount;

    private String goodsAmountText;

    private String freightAmountText;

    private String totalAmountText;

    private String courierCompany;

    private String trackingNo;

    /** 支付流水（已支付订单才有，来自 payment_record） */
    private boolean hasPayment;

    private String paymentTransactionNo;

    private String paymentMethod;

    private String paymentTimeText;

    private String paymentAmountText;

    private List<TimelineNode> timeline = new ArrayList<>();

    private boolean canPay;

    private boolean canCancel;

    private boolean canReceive;

    private boolean canDelete;

    private boolean canComplaint;

    public static OrderDetailVO of(Order order, List<OrderItem> lines) {
        return of(order, lines, null);
    }

    /** 后台版本：多带一个下单用户名 */
    public static OrderDetailVO of(Order order, List<OrderItem> lines, String username) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setUsername(username);
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatus.descOf(order.getStatus()));
        vo.setStatusClass(OrderSummaryVO.badgeClassOf(order.getStatus()));
        vo.setCreateTimeText(format(order.getCreateTime()));
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhoneMasked(AccountProfileVO.maskPhone(order.getReceiverPhone()));
        // 后台详情页要用完整手机号回填「修改收货信息」表单；前台走两参重载，这里保持 null
        vo.setReceiverPhoneRaw(order.getReceiverPhone());
        vo.setReceiverAddress(joinAddress(order));
        vo.setReceiverProvince(order.getReceiverProvince());
        vo.setReceiverCity(order.getReceiverCity());
        vo.setReceiverDistrict(order.getReceiverDistrict());
        vo.setReceiverDetail(order.getReceiverDetail());
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
     * 填充支付流水：只有支付成功且流水存在时才展示，历史订单缺流水时页面不显示该区块。
     */
    public void applyPayment(PaymentRecord payment) {
        if (payment == null) {
            return;
        }
        this.hasPayment = true;
        this.paymentTransactionNo = payment.getTransactionNo();
        this.paymentMethod = methodDesc(payment.getPayMethod());
        this.paymentTimeText = format(payment.getPayTime());
        this.paymentAmountText = OrderItemVO.text(payment.getAmount());
    }

    private static String methodDesc(String method) {
        return PaymentRecord.METHOD_MOCK.equals(method) ? "模拟支付" : method;
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
