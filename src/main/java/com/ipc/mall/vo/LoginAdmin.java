package com.ipc.mall.vo;

import com.ipc.mall.entity.Admin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 放入 Session 的后台登录态，与前台 {@link LoginAccount} 完全隔离。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginAdmin implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long adminId;

    private String username;

    private String nickname;

    public static LoginAdmin of(Admin admin) {
        String nickname = admin.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = admin.getUsername();
        }
        return new LoginAdmin(admin.getId(), admin.getUsername(), nickname);
    }

    public String getDisplayName() {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }
}
