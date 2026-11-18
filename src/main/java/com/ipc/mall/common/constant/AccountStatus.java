package com.ipc.mall.common.constant;

/**
 * 前台/后台账号状态（account.status、admin.status 均为 TINYINT，PRD §7.3）。
 */
public final class AccountStatus {

    /** 启用，可登录 */
    public static final int ENABLED = 1;

    /** 禁用，登录时直接拒绝 */
    public static final int DISABLED = 0;

    private AccountStatus() {
    }

    public static boolean isEnabled(Integer status) {
        return status != null && status == ENABLED;
    }
}
