package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.Order;

/**
 * 订单 Mapper。
 * <p>
 * 状态流转必须用<b>条件更新</b>保证幂等（PRD §8.3 第 4 条）：
 * {@code UPDATE orders SET status = #{to} WHERE id = #{id} AND status = #{from}}，
 * 影响行数为 0 就返回 3002，不要先查再改。
 * 示例（取消订单）：
 * <pre>
 * int rows = orderMapper.update(null, Wrappers.&lt;Order&gt;lambdaUpdate()
 *         .set(Order::getStatus, CANCELLED.name())
 *         .set(Order::getCancelTime, LocalDateTime.now())
 *         .eq(Order::getId, id)
 *         .eq(Order::getStatus, PENDING_PAYMENT.name()));
 * </pre>
 */
public interface OrderMapper extends BaseMapper<Order> {
}
