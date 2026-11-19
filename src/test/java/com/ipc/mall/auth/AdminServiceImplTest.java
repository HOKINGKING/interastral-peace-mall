package com.ipc.mall.auth;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.entity.Admin;
import com.ipc.mall.mapper.AdminMapper;
import com.ipc.mall.service.impl.AdminServiceImpl;
import com.ipc.mall.vo.LoginAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 后台账号认证规则（PRD §6.1）。
 */
class AdminServiceImplTest {

    private AdminMapper adminMapper;
    private BCryptPasswordEncoder passwordEncoder;
    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminMapper = mock(AdminMapper.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        adminService = new AdminServiceImpl(adminMapper, passwordEncoder);
    }

    @Test
    void 登录成功更新最后登录时间() {
        Admin admin = admin(1L, "admin", "123456", AccountStatus.ENABLED);
        when(adminMapper.selectOne(any(Wrapper.class))).thenReturn(admin);

        LoginAdmin loginAdmin = adminService.login("admin", "123456");

        assertEquals(1L, loginAdmin.getAdminId());
        assertEquals("平台管理员", loginAdmin.getDisplayName());
        verify(adminMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void 密码错误返回五千零一() {
        Admin admin = admin(1L, "admin", "123456", AccountStatus.ENABLED);
        when(adminMapper.selectOne(any(Wrapper.class))).thenReturn(admin);

        BizException exception = assertThrows(BizException.class,
                () -> adminService.login("admin", "wrong"));
        assertEquals(ResultCode.ADMIN_LOGIN_FAILED.getCode(), exception.getCode());
    }

    @Test
    void 禁用后台账号返回一千零四() {
        Admin admin = admin(1L, "admin", "123456", AccountStatus.DISABLED);
        when(adminMapper.selectOne(any(Wrapper.class))).thenReturn(admin);

        BizException exception = assertThrows(BizException.class,
                () -> adminService.login("admin", "123456"));
        assertEquals(ResultCode.ACCOUNT_DISABLED.getCode(), exception.getCode());
    }

    private Admin admin(Long id, String username, String rawPassword, int status) {
        Admin admin = new Admin();
        admin.setId(id);
        admin.setUsername(username);
        admin.setNickname("平台管理员");
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setStatus(status);
        return admin;
    }
}
