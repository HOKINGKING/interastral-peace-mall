package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.OrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 订单明细 Mapper：按 orderId 查明细即可，明细只插入不更新 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 该商品（SPU）是否被未完结订单引用（PRD §6.3 A13：删除 SPU 前校验）。
     * <p>
     * 未完结 = PENDING_PAYMENT / PAID / SHIPPED；已取消、已完成的订单不挡删除。
     * 订单已存快照，这条限制是业务上的保守策略。
     */
    @Select("SELECT COUNT(*) FROM order_item oi JOIN orders o ON o.id = oi.order_id "
            + "WHERE oi.product_id = #{productId} AND o.deleted = 0 "
            + "AND o.status IN ('PENDING_PAYMENT', 'PAID', 'SHIPPED')")
    long countUnfinishedByProduct(@Param("productId") Long productId);
}
