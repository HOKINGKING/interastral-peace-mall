package com.ipc.mall.common.constant;

import java.util.Map;
import java.util.Set;

/**
 * 订单状态常量与状态机。
 * <p>
 * 数据库 sales_order.status 是 VARCHAR(16) + CHECK 约束，并额外用 CHECK 保证
 * "状态与时间字段"的一致性（例如 PAID 必须有 paid_at）。Java 侧只负责<b>业务规则</b>：
 * 哪些状态之间允许迁移，由 {@link #canTransit(String, String)} 统一判断，
 * 避免业务代码里散落 if-else。
 * <pre>
 * PENDING_PAYMENT ──支付──> PAID ──发货──> SHIPPED ──确认收货──> COMPLETED
 *        └────────取消────────> CANCELLED
 * </pre>
 */
public final class OrderStatus {

    /** 待支付：下单后默认状态，占用库存 */
    public static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    /** 已支付（模拟支付成功），等待发货 */
    public static final String PAID = "PAID";
    /** 已发货 */
    public static final String SHIPPED = "SHIPPED";
    /** 已完成（顾客确认收货） */
    public static final String COMPLETED = "COMPLETED";
    /** 已取消：仅待支付订单可取消，取消后恢复库存 */
    public static final String CANCELLED = "CANCELLED";

    private static final Map<String, String> DESC = Map.of(
            PENDING_PAYMENT, "待支付",
            PAID, "已支付",
            SHIPPED, "已发货",
            COMPLETED, "已完成",
            CANCELLED, "已取消"
    );

    private static final Map<String, Set<String>> NEXT = Map.of(
            PENDING_PAYMENT, Set.of(PAID, CANCELLED),
            PAID, Set.of(SHIPPED),
            SHIPPED, Set.of(COMPLETED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of()
    );

    private OrderStatus() {
    }

    /** 状态中文名，页面展示用 */
    public static String descOf(String status) {
        return DESC.getOrDefault(String.valueOf(status), status);
    }

    /** 判断状态迁移是否合法，非法一律拒绝，不要靠数据库 CHECK 兜底 */
    public static boolean canTransit(String from, String to) {
        return NEXT.getOrDefault(String.valueOf(from), Set.of()).contains(to);
    }

    /** 是否终态（不可再变更） */
    public static boolean isFinal(String status) {
        return COMPLETED.equals(status) || CANCELLED.equals(status);
    }
}
