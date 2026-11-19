package com.ipc.mall.common.constant;

/**
 * Session 属性名统一入口（PRD §3.1 / §5.7）。
 * <p>
 * 前后台共用同一个 JSESSIONID，靠不同的 key + 不同的拦截器路径隔离；
 * 退出或改密时只能删除自己的 key，绝对不能 {@code session.invalidate()}，
 * 否则会把同一浏览器里的另一端登录态一起踢掉。
 */
public final class SessionKey {

    /** 前台登录态，值为 {@code LoginAccount} */
    public static final String LOGIN_ACCOUNT = "LOGIN_ACCOUNT";

    /** 后台登录态，值为 {@code LoginAdmin} */
    public static final String LOGIN_ADMIN = "LOGIN_ADMIN";

    /** 前台登录态最后一次回查数据库状态的时间戳（毫秒） */
    public static final String LOGIN_ACCOUNT_CHECK_AT = "LOGIN_ACCOUNT_CHECK_AT";

    /** 后台登录态最后一次回查数据库状态的时间戳（毫秒） */
    public static final String LOGIN_ADMIN_CHECK_AT = "LOGIN_ADMIN_CHECK_AT";

    /** 前台验证码字符 */
    public static final String CAPTCHA_CODE = "CAPTCHA_CODE";

    /** 前台验证码过期时间（毫秒时间戳） */
    public static final String CAPTCHA_EXPIRE_AT = "CAPTCHA_EXPIRE_AT";

    /** 后台验证码字符，独立 key，避免同浏览器前后台互相覆盖 */
    public static final String ADMIN_CAPTCHA_CODE = "ADMIN_CAPTCHA_CODE";

    /** 后台验证码过期时间（毫秒时间戳） */
    public static final String ADMIN_CAPTCHA_EXPIRE_AT = "ADMIN_CAPTCHA_EXPIRE_AT";

    private SessionKey() {
    }
}
