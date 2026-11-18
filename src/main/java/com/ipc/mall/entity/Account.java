package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ipc.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 前台用户（PRD §7.3 account）。
 * 后台卖家账号是独立的 {@code admin} 表，不靠 role 字段区分。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("account")
public class Account extends BaseEntity {

    /** 登录名，20 字以内 */
    private String username;

    /** BCrypt 密文（定长 60），绝不存明文 */
    private String password;

    /** 昵称，缺省展示 username */
    private String nickname;

    /** 手机号，11 位，可空 */
    private String phone;

    /** 1 启用 / 0 禁用，见 AccountStatus */
    private Integer status;

    private LocalDateTime lastLoginTime;
}
