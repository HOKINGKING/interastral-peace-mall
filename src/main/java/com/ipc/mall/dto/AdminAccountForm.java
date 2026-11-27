package com.ipc.mall.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 后台用户编辑表单（PRD §6.4 / docs/api.md A20）。
 * <p>
 * 只允许改<b>昵称 / 手机号 / 状态</b>：用户名是登录标识，密码走 A21 的重置入口，
 * 两者都不在这个表单里，避免后台误改导致用户无法登录。
 */
@Data
public class AdminAccountForm {

    /** 必填：编辑既有用户，不存在则返回 400 */
    @NotNull(message = "缺少用户 id")
    private Long id;

    /** 选填；空串由 Service 归一化为 null，页面回落显示用户名 */
    @Size(max = 20, message = "昵称最多 20 个字符")
    private String nickname;

    /** 选填；空串表示解绑手机号 */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号需为 11 位数字")
    private String phone;

    /** 1 启用 / 0 禁用，见 AccountStatus */
    @NotNull(message = "请选择账号状态")
    @Min(value = 0, message = "账号状态只能是 0 或 1")
    @Max(value = 1, message = "账号状态只能是 0 或 1")
    private Integer status;
}
