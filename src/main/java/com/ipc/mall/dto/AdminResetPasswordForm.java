package com.ipc.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 后台重置用户密码（PRD §6.4 / docs/api.md A21）。
 * <p>
 * 强度规则与注册保持一致（6–20 位且同时包含字母和数字），
 * 由 Service 用 BCrypt 重新加密后写库，明文不落库、不进日志。
 */
@Data
public class AdminResetPasswordForm {

    @NotBlank(message = "请输入新密码")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,20}$",
            message = "密码需为 6–20 位且同时包含字母和数字")
    private String password;
}
