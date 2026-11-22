package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 订单 Mapper。
 * <p>
 * 状态流转必须用<b>条件更新</b>保证幂等（PRD §8.3 第 4 条）：
 * {@code WHERE id=? AND account_id=? AND status=<from>}，
 * 影响行数为 0 就返回 3002，不要先查再改。
 * <p>
 * 「允许从哪个状态迁到哪个状态」由 {@link com.ipc.mall.common.constant.OrderStatus#canTransitTo}
 * 在 Service 里判定；这里的 WHERE 是并发兜底，两者缺一不可。
 */
public interface OrderMapper extends BaseMapper<Order> {

    /** T2 支付成功：只允许 PENDING_PAYMENT → PAID，同时写 pay_time */
    @Update("UPDATE orders SET status = 'PAID', pay_time = #{payTime}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND account_id = #{accountId} AND status = 'PENDING_PAYMENT' AND deleted = 0")
    int markPaid(@Param("id") Long id,
                 @Param("accountId") Long accountId,
                 @Param("payTime") LocalDateTime payTime,
                 @Param("updateTime") LocalDateTime updateTime);

    /** T3 用户取消：只允许 PENDING_PAYMENT → CANCELLED，同时写 cancel_time */
    @Update("UPDATE orders SET status = 'CANCELLED', cancel_time = #{cancelTime}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND account_id = #{accountId} AND status = 'PENDING_PAYMENT' AND deleted = 0")
    int markCancelled(@Param("id") Long id,
                      @Param("accountId") Long accountId,
                      @Param("cancelTime") LocalDateTime cancelTime,
                      @Param("updateTime") LocalDateTime updateTime);

    /** T6 确认收货：只允许 SHIPPED → COMPLETED，同时写 finish_time */
    @Update("UPDATE orders SET status = 'COMPLETED', finish_time = #{finishTime}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND account_id = #{accountId} AND status = 'SHIPPED' AND deleted = 0")
    int markCompleted(@Param("id") Long id,
                      @Param("accountId") Long accountId,
                      @Param("finishTime") LocalDateTime finishTime,
                      @Param("updateTime") LocalDateTime updateTime);
}
