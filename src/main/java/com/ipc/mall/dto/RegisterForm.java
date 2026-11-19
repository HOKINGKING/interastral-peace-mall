package com.ipc.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册表单（PRD §5.5）。
 */
@Data
public class RegisterForm {

    @NotBlank(message = "请输入用户名")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "用户名需为 4–20 位字母、数字或下划线")
    private String username;

    @NotBlank(message = "请输入密码")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,20}$", message = "密码需为 6–20 位且同时包含字母和数字")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    /** 选填；空串也允许，服务层会归一化为 null */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号需为 11 位数字")
    private String phone;

    @NotBlank(message = "请输入验证码")
    @Size(min = 4, max = 4, message = "验证码为 4 位字符")
    private String captcha;
}
