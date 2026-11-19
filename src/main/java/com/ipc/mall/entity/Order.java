package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ipc.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单（PRD §7.3 orders / §8 状态机）。
 * <p>
 * 类名用 {@code Order}，表名必须是 {@code orders}（ORDER 是 MySQL 关键字）。
 * 收货信息是<b>快照</b>：下单时从 address 复制过来，地址后续修改不回写。
 * 状态字段存枚举名，流转一律走 {@link com.ipc.mall.common.constant.OrderStatus#canTransitTo}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Order extends BaseEntity {

    /** 业务单号：yyyyMMddHHmmss + 4 位随机数，唯一 */
    private String orderNo;

    private Long accountId;

    /** 应付总额 = 商品总额 + 运费 */
    private BigDecimal totalAmount;

    /** 运费，本期固定 0.00 */
    private BigDecimal freightAmount;

    /** PENDING_PAYMENT / PAID / SHIPPED / COMPLETED / CANCELLED */
    private String status;

    private String receiverName;

    private String receiverPhone;

    private String receiverProvince;

    private String receiverCity;

    private String receiverDistrict;

    private String receiverDetail;

    /** 物流公司，卖家发货时写入 */
    private String courierCompany;

    /** 物流单号，卖家发货时写入 */
    private String trackingNo;

    /** 订单备注 / 后台备注 */
    private String remark;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime finishTime;

    private LocalDateTime cancelTime;

    /** 是否处于待支付，前端展示倒计时用 */
    public boolean isPendingPayment() {
        return "PENDING_PAYMENT".equals(status);
    }
}
