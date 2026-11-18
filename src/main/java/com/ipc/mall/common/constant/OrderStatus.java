package com.ipc.mall.common.constant;

import lombok.Getter;

/**
 * 订单状态枚举（PRD §8 为唯一权威定义）。
 * <p>
 * 数据库 orders.status 存的是<b>枚举名</b>（VARCHAR(20)），不是数字，
 * 所以实体字段用 String，业务里用 {@code OrderStatus.PAID.name()} 比较或赋值。
 * 所有状态流转必须走 {@link #canTransitTo(OrderStatus)}，禁止直接 set 字符串。
 * <pre>
 * PENDING_PAYMENT ──支付──▶ PAID ──发货──▶ SHIPPED ──确认收货──▶ COMPLETED
 *        └────用户取消 / 超时关单────▶ CANCELLED
 * </pre>
 */
@Getter
public enum OrderStatus {

    PENDING_PAYMENT("待支付"),
    PAID("已支付"),
    SHIPPED("已发货"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String desc;

    OrderStatus(String desc) {
        this.desc = desc;
    }

    /** 是否允许从当前状态迁移到目标状态（PRD §8.2 转移表） */
    public boolean canTransitTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PENDING_PAYMENT -> target == PAID || target == CANCELLED;
            case PAID -> target == SHIPPED;
            case SHIPPED -> target == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    /** 终态不可再变更 */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /** 由数据库里的字符串还原枚举，非法值抛业务异常 */
    public static OrderStatus of(String code) {
        for (OrderStatus s : values()) {
            if (s.name().equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("非法的订单状态: " + code);
    }

    /** 页面展示用中文名，未知值原样返回 */
    public static String descOf(String code) {
        for (OrderStatus s : values()) {
            if (s.name().equals(code)) {
                return s.desc;
            }
        }
        return code;
    }
}
