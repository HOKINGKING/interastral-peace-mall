package com.ipc.mall.common.constant;

/**
 * 账号角色与状态常量（对应 store_user.role / store_user.status）。
 */
public final class UserConst {

    /** 前台顾客 */
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    /** 后台管理员 */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 正常可用 */
    public static final String STATUS_ENABLED = "ENABLED";
    /** 已禁用：登录时直接拒绝 */
    public static final String STATUS_DISABLED = "DISABLED";

    private UserConst() {
    }

    public static boolean isAdmin(String role) {
        return ROLE_ADMIN.equals(role);
    }

    public static boolean isEnabled(String status) {
        return STATUS_ENABLED.equals(status);
    }
}
