package com.ipc.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录表单（PRD §5.6）。后台登录复用本表单，仅忽略 redirect。
 */
@Data
public class LoginForm {

    @NotBlank(message = "请输入用户名")
    private String username;

    @NotBlank(message = "请输入密码")
    private String password;

    @NotBlank(message = "请输入验证码")
    @Size(min = 4, max = 4, message = "验证码为 4 位字符")
    private String captcha;

    /** 登录成功后的站内跳转目标，进入页面前会做白名单校验 */
    private String redirect;
}
