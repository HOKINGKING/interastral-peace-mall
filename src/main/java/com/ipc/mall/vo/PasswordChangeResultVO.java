package com.ipc.mall.vo;

import lombok.Data;

/**
 * 修改密码异步结果：出错时告诉前端应该高亮哪个字段，成功时给出跳转地址。
 */
@Data
public class PasswordChangeResultVO {

    /** 出错的字段名：oldPassword / newPassword / confirmPassword；成功时为 null */
    private String field;

    /** 成功后的跳转地址 */
    private String redirect;

    public static PasswordChangeResultVO field(String field) {
        PasswordChangeResultVO vo = new PasswordChangeResultVO();
        vo.setField(field);
        return vo;
    }

    public static PasswordChangeResultVO redirect(String redirect) {
        PasswordChangeResultVO vo = new PasswordChangeResultVO();
        vo.setRedirect(redirect);
        return vo;
    }
}
