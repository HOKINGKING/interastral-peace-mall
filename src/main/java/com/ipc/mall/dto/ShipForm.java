package com.ipc.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台发货表单（PRD §6.5 / docs/api.md A24）。
 * <p>
 * 仅 PAID 可发货；物流公司与单号都必填。
 */
@Data
public class ShipForm {

    @NotBlank(message = "请输入物流公司")
    @Size(max = 30, message = "物流公司最多 30 个字符")
    private String courierCompany;

    @NotBlank(message = "请输入物流单号")
    @Size(max = 40, message = "物流单号最多 40 个字符")
    private String trackingNo;
}
