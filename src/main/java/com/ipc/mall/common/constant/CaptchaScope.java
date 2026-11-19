package com.ipc.mall.common.constant;

/**
 * 验证码作用域：前台与后台使用不同的 Session key（PRD §5.7）。
 */
public enum CaptchaScope {

    FRONT(SessionKey.CAPTCHA_CODE, SessionKey.CAPTCHA_EXPIRE_AT),
    ADMIN(SessionKey.ADMIN_CAPTCHA_CODE, SessionKey.ADMIN_CAPTCHA_EXPIRE_AT);

    private final String codeKey;
    private final String expireKey;

    CaptchaScope(String codeKey, String expireKey) {
        this.codeKey = codeKey;
        this.expireKey = expireKey;
    }

    public String getCodeKey() {
        return codeKey;
    }

    public String getExpireKey() {
        return expireKey;
    }
}
