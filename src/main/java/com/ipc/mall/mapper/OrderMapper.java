package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.entity.Order;
import com.ipc.mall.vo.AdminOrderRowVO;
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

    /**
     * T5 卖家发货：只允许 PAID → SHIPPED，写 ship_time / 物流公司 / 单号。
     * 后台没有 account_id 维度（单一角色），重复发货靠 status='PAID' 条件兜住。
     */
    @Update("UPDATE orders SET status = 'SHIPPED', courier_company = #{courierCompany}, "
            + "tracking_no = #{trackingNo}, ship_time = #{shipTime}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND status = 'PAID' AND deleted = 0")
    int markShipped(@Param("id") Long id,
                    @Param("courierCompany") String courierCompany,
                    @Param("trackingNo") String trackingNo,
                    @Param("shipTime") LocalDateTime shipTime,
                    @Param("updateTime") LocalDateTime updateTime);

    /**
     * A25 后台改收货信息：只动收货快照与备注，金额与明细锁定。
     * 不影响状态机，任何未删除状态都可以改。
     */
    @Update("UPDATE orders SET receiver_name = #{receiverName}, receiver_phone = #{receiverPhone}, "
            + "receiver_detail = #{receiverDetail}, remark = #{remark}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND deleted = 0")
    int updateReceiver(@Param("id") Long id,
                       @Param("receiverName") String receiverName,
                       @Param("receiverPhone") String receiverPhone,
                       @Param("receiverDetail") String receiverDetail,
                       @Param("remark") String remark,
                       @Param("updateTime") LocalDateTime updateTime);

    /** 后台订单分页（A22）：按订单号 / 用户名 / 状态 / 下单时间区间筛选 */
    IPage<AdminOrderRowVO> selectAdminPage(IPage<AdminOrderRowVO> page,
                                           @Param("orderNo") String orderNo,
                                           @Param("username") String username,
                                           @Param("status") String status,
                                           @Param("dateFrom") LocalDateTime dateFrom,
                                           @Param("dateTo") LocalDateTime dateTo);
}
