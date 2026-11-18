package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ipc.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 后台卖家账号（PRD §7.3 admin）。
 * 与前台 account 分表：登录入口、会话 key、拦截器规则都不同。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin")
public class Admin extends BaseEntity {

    private String username;

    /** BCrypt 密文 */
    private String password;

    private String nickname;

    /** 1 启用 / 0 禁用 */
    private Integer status;

    private LocalDateTime lastLoginTime;
}
