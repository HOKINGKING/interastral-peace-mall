package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.PaymentRecord;

/**
 * 支付流水 Mapper。
 * <p>
 * 只需 BaseMapper：按订单查询用 Wrapper 即可，没有复杂 SQL。
 */
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
