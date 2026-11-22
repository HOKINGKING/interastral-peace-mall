package com.ipc.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址表单（PRD §5.10 / docs/api.md F21）。
 * <p>
 * 新增与编辑共用：{@code id} 为空是新增，有值是编辑。
 * {@code isDefault} 故意用 String：前端可能传 {@code "on"}（checkbox）、{@code "1"}、{@code ""} 或 null，
 * 由 Service 统一归一化成 0/1（handoff §6 第 11 条）。
 */
@Data
public class AddressForm {

    /** 编辑时的地址主键；新增为空 */
    private Long id;

    @NotBlank(message = "请输入收货人")
    @Size(max = 20, message = "收货人最多 20 个字符")
    private String receiver;

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号需为 11 位数字")
    private String phone;

    @Size(max = 20, message = "省份最多 20 个字符")
    private String province;

    @Size(max = 20, message = "城市最多 20 个字符")
    private String city;

    @Size(max = 20, message = "区县最多 20 个字符")
    private String district;

    @NotBlank(message = "请输入详细地址")
    @Size(max = 100, message = "详细地址最多 100 个字符")
    private String detail;

    /** 是否默认：1 默认 / 0 普通，Service 归一化 */
    private String isDefault;
}
