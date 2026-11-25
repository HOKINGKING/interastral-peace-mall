package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ipc.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水（PRD §7.3 payment_record / §5.12）。
 * <p>
 * 每次「模拟支付成功」写一条，是订单支付行为的独立凭证：
 * <ul>
 *     <li>{@code order_id} 唯一：一笔订单最多一条成功流水，支付接口幂等；</li>
 *     <li>{@code transaction_no} 唯一：对账用的支付流水号；</li>
 *     <li>金额存支付发生时的订单应付快照，后续改价不影响历史流水。</li>
 * </ul>
 * 记录只增不改，逻辑删除字段保留只为与 BaseEntity 一致，业务上不删除。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_record")
public class PaymentRecord extends BaseEntity {

    /** 支付方式：MOCK 模拟支付（本期唯一值） */
    public static final String METHOD_MOCK = "MOCK";

    /** 流水状态：SUCCESS 支付成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";

    private Long orderId;

    /** 订单号快照，对账时不必回查 orders */
    private String orderNo;

    private Long accountId;

    /** 支付金额（订单应付快照） */
    private BigDecimal amount;

    /** MOCK */
    private String payMethod;

    /** SUCCESS */
    private String status;

    /** 支付流水号，唯一 */
    private String transactionNo;

    private LocalDateTime payTime;
}
