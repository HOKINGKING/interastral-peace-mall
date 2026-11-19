package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.entity.Admin;
import com.ipc.mall.mapper.AdminMapper;
import com.ipc.mall.service.AdminService;
import com.ipc.mall.vo.LoginAdmin;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 后台账号认证实现（PRD §6.1）：与前台账号完全分表、分会话 key。
 */
@Service
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(AdminMapper adminMapper, PasswordEncoder passwordEncoder) {
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginAdmin login(String username, String password) {
        String loginName = username == null ? "" : username.trim();
        Admin admin = adminMapper.selectOne(
                Wrappers.<Admin>query().eq("username", loginName));

        if (admin == null || password == null
                || !passwordEncoder.matches(password, admin.getPassword())) {
            throw new BizException(ResultCode.ADMIN_LOGIN_FAILED);
        }
        if (!AccountStatus.isEnabled(admin.getStatus())) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }

        adminMapper.update(null, Wrappers.<Admin>update()
                .eq("id", admin.getId())
                .set("last_login_time", LocalDateTime.now()));
        return LoginAdmin.of(admin);
    }
}
