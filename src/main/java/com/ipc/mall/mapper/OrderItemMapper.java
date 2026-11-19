package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.OrderItem;

/** 订单明细 Mapper：按 orderId 查明细即可，明细只插入不更新 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
