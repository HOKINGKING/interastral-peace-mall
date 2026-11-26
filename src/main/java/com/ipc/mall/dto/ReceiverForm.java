package com.ipc.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台修改收货信息（PRD §6.5 / docs/api.md A25）。
 * <p>
 * 只允许改收货人 / 电话 / 详细地址（省市区沿用下单快照）与备注；
 * 订单金额与商品明细是快照，锁定不可改。
 */
@Data
public class ReceiverForm {

    @NotBlank(message = "请输入收货人")
    @Size(max = 20, message = "收货人最多 20 个字符")
    private String receiverName;

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号需为 11 位数字")
    private String receiverPhone;

    @NotBlank(message = "请输入详细地址")
    @Size(max = 100, message = "详细地址最多 100 个字符")
    private String receiverDetail;

    /** 选填；null 表示不改，空串表示清空 */
    @Size(max = 200, message = "备注最多 200 字")
    private String remark;
}
