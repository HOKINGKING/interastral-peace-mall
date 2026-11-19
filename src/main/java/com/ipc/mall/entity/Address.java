package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ipc.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收货地址（PRD §7.3 address）。
 * 下单时把地址字段复制进 orders 的 receiver_* 快照列，之后地址被改不影响历史订单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address")
public class Address extends BaseEntity {

    private Long accountId;

    /** 收货人 */
    private String receiver;

    /** 手机号，11 位 */
    private String phone;

    private String province;

    private String city;

    private String district;

    /** 详细地址，不含省市区 */
    private String detail;

    /** 1 默认地址 / 0 普通 */
    private Integer isDefault;
}
