package com.ipc.mall.service;

import com.ipc.mall.dto.ChangePasswordForm;
import com.ipc.mall.dto.RegisterForm;
import com.ipc.mall.vo.AccountProfileVO;
import com.ipc.mall.vo.LoginAccount;

/**
 * 前台账号认证服务（PRD §5.5–5.8）。
 */
public interface AccountService {

    /** 注册成功返回登录态，由 Controller 写入 Session */
    LoginAccount register(RegisterForm form);

    /** 登录成功返回登录态，并已更新 last_login_time */
    LoginAccount login(String username, String password);

    /** 个人中心资料 */
    AccountProfileVO getProfile(Long accountId);

    /** 修改密码；成功后由 Controller 清除前台登录态 */
    void changePassword(Long accountId, ChangePasswordForm form);
}
