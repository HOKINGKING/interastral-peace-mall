package com.ipc.mall.common.result;

import lombok.Getter;

/**
 * 统一返回状态码
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(400, "操作失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    ERROR(500, "服务器内部错误"),

    // ---------- 认证模块（PRD §10.4） ----------
    USERNAME_EXISTS(1001, "用户名已存在"),
    LOGIN_FAILED(1002, "用户名或密码错误"),
    CAPTCHA_ERROR(1003, "验证码错误或已过期"),
    ACCOUNT_DISABLED(1004, "账号已被禁用"),
    OLD_PASSWORD_ERROR(1005, "原密码不正确"),

    // ---------- 后台登录（PRD §10.4） ----------
    ADMIN_LOGIN_FAILED(5001, "后台账号或密码错误"),

    // ---------- 商品 / 购物车 / 订单（PRD §10.4） ----------
    PRODUCT_OFF_SALE(2001, "商品不存在或已下架"),
    STOCK_NOT_ENOUGH(2002, "库存不足"),
    NO_PERMISSION(3004, "无权操作该资源");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
