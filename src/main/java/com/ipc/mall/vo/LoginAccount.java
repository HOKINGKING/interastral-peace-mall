package com.ipc.mall.vo;

import com.ipc.mall.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 放入 Session 的前台登录态，只保留页面需要的最小字段。
 * 不要把 {@link Account} 实体放进 Session：它带密码密文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long accountId;

    private String username;

    private String nickname;

    public static LoginAccount of(Account account) {
        String nickname = account.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = account.getUsername();
        }
        return new LoginAccount(account.getId(), account.getUsername(), nickname);
    }

    /** 页面展示名：昵称为空时退回用户名 */
    public String getDisplayName() {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }
}
