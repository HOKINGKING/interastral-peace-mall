package com.ipc.mall.service;

import com.ipc.mall.vo.LoginAdmin;

/**
 * 后台账号认证服务（PRD §6.1）。
 */
public interface AdminService {

    /** 后台登录成功返回登录态，并已更新 last_login_time */
    LoginAdmin login(String username, String password);
}
