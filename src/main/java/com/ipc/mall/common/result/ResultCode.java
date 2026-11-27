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

    // ---------- 上传（PRD §10.4） ----------
    UPLOAD_INVALID(4001, "文件类型不支持或超过大小限制"),

    // ---------- 商品 / 购物车 / 订单（PRD §10.4） ----------
    PRODUCT_OFF_SALE(2001, "商品不存在或已下架"),
    STOCK_NOT_ENOUGH(2002, "库存不足"),
    CATEGORY_IN_USE(2004, "存在子分类或商品，无法删除"),
    PRODUCT_IN_USE(2005, "存在未完结订单，无法删除"),
    NO_PERMISSION(3004, "无权操作该资源"),

    // ---------- 订单（PRD §10.4 / §8） ----------
    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_STATUS_INVALID(3002, "当前状态不允许该操作"),
    ORDER_NOT_OWNED(3003, "无权操作该订单"),
    CART_EMPTY(3005, "请选择要结算的商品"),
    DUPLICATE_SUBMIT(3006, "请勿重复提交"),

    // ---------- 投诉与反馈（PRD §5.15 / §8.4） ----------
    FEEDBACK_ORDER_INVALID(3007, "只能投诉本人已支付/已发货/已完成的订单"),
    FEEDBACK_DUPLICATE(3008, "该订单已有未关闭的投诉"),
    FEEDBACK_RATE_LIMIT(3009, "反馈提交过于频繁，请稍后再试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
