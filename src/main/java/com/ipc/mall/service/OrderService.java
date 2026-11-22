package com.ipc.mall.service;

import com.ipc.mall.dto.OrderSubmitForm;
import com.ipc.mall.entity.Order;
import com.ipc.mall.vo.CheckoutVO;

/**
 * 交易链路：结算下单 / 模拟支付 / 订单管理（PRD §5.11–§5.14、§8 / docs/api.md F24–F31、F37）。
 * <p>
 * 订单类操作的越权码是 <b>3003</b>（3004 是地址），订单不存在是 3001。
 */
public interface OrderService {

    /**
     * 确认订单页数据（F24）：购物车勾选或立即购买两种来源归一成同一份明细，并按数据库现价重算。
     *
     * @param cartIds 逗号分隔的购物车行 id；非空时忽略 itemId/quantity
     */
    CheckoutVO checkout(Long accountId, String cartIds, Long itemId, Integer quantity);

    /**
     * 提交订单（F25）：校验 → 落订单与明细 → 扣库存 → 清购物车，全程同一事务。
     *
     * @return 新订单 id
     */
    Long submit(Long accountId, OrderSubmitForm form);

    /** 支付页 / 状态校验用：订单不存在 3001，非本人 3003 */
    Order get(Long accountId, Long orderId);

    /** 模拟支付（T2）：PENDING_PAYMENT → PAID + pay_time + 累加销量；已 PAID 幂等直接返回 */
    void pay(Long accountId, Long orderId);

    /** 取消（T3）：PENDING_PAYMENT → CANCELLED + cancel_time + 回补库存；其它状态 3002 */
    void cancel(Long accountId, Long orderId);

    /** 确认收货（T6）：SHIPPED → COMPLETED + finish_time；其它状态 3002 */
    void receive(Long accountId, Long orderId);
}
