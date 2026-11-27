package com.ipc.mall.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.AdminAccountForm;
import com.ipc.mall.entity.Account;
import com.ipc.mall.mapper.AccountMapper;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.service.impl.AccountAdminServiceImpl;
import com.ipc.mall.vo.AdminAccountDetailVO;
import com.ipc.mall.vo.AdminAccountRowVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 后台用户管理（PRD §6.4 / A18–A21）：列表映射、详情订单数、编辑约束、密码重置。
 */
class AccountAdminServiceImplTest {

    private AccountMapper accountMapper;
    private OrderMapper orderMapper;
    private PasswordEncoder passwordEncoder;
    private AccountAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        accountMapper = mock(AccountMapper.class);
        orderMapper = mock(OrderMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new AccountAdminServiceImpl(accountMapper, orderMapper, passwordEncoder);
    }

    // ------------------------------------------------------------ 列表

    @Test
    void 列表把账号映射成行并打码手机号() {
        Page<Account> raw = new Page<>(1, 10, 1);
        raw.setRecords(List.of(account(1L, "xingye", "星野", "13800000001", 1)));
        doReturn(raw).when(accountMapper).selectPage(any(), any());

        IPage<AdminAccountRowVO> page = service.page(" 138 ", 1, 1, 10);

        assertEquals(1, page.getTotal());
        AdminAccountRowVO row = page.getRecords().get(0);
        assertEquals("xingye", row.getUsername());
        assertEquals("星野", row.getDisplayName());
        assertEquals("138****0001", row.getPhoneMasked());
        assertEquals("启用", row.getStatusDesc());
        assertEquals("badge-completed", row.getStatusClass());
    }

    @Test
    void 列表状态非法时按全部处理() {
        Page<Account> raw = new Page<>(1, 10, 0);
        doReturn(raw).when(accountMapper).selectPage(any(), any());

        IPage<AdminAccountRowVO> page = service.page(null, 9, 1, 10);

        assertTrue(page.getRecords().isEmpty());
        verify(accountMapper).selectPage(any(), any());
    }

    @Test
    void 昵称为空时回落用户名且禁用显示红色徽标() {
        Page<Account> raw = new Page<>(1, 10, 1);
        raw.setRecords(List.of(account(2L, "bob", null, "13800000002", 0)));
        doReturn(raw).when(accountMapper).selectPage(any(), any());

        AdminAccountRowVO row = service.page(null, 0, 1, 10).getRecords().get(0);

        assertNull(row.getNickname());
        assertEquals("bob", row.getDisplayName());
        assertEquals("禁用", row.getStatusDesc());
        assertEquals("badge-danger", row.getStatusClass());
    }

    @Test
    void 未绑定手机号时显示未绑定() {
        Page<Account> raw = new Page<>(1, 10, 1);
        raw.setRecords(List.of(account(3L, "carol", null, null, 1)));
        doReturn(raw).when(accountMapper).selectPage(any(), any());

        AdminAccountRowVO row = service.page(null, null, 1, 10).getRecords().get(0);

        assertEquals("未绑定", row.getPhoneMasked());
    }

    // ------------------------------------------------------------ 详情

    @Test
    void 详情带出订单数与完整手机号() {
        when(accountMapper.selectById(1L)).thenReturn(account(1L, "xingye", "星野", "13800000001", 1));
        when(orderMapper.selectCount(any())).thenReturn(3L);

        AdminAccountDetailVO detail = service.detail(1L);

        assertEquals("xingye", detail.getUsername());
        assertEquals("13800000001", detail.getPhone());
        assertEquals("138****0001", detail.getPhoneMasked());
        assertEquals(3L, detail.getOrderCount());
        assertTrue(detail.getCreateTimeText().startsWith("2026-11-17"));
        assertEquals("启用", detail.getStatusDesc());
    }

    @Test
    void 详情不存在返回四百() {
        when(accountMapper.selectById(9L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> service.detail(9L));

        assertEquals(ResultCode.FAIL.getCode(), exception.getCode());
        assertEquals("用户不存在或已被删除", exception.getMessage());
    }

    // ------------------------------------------------------------ 编辑

    @Test
    void 保存只更新昵称手机号与状态() {
        when(accountMapper.selectById(1L)).thenReturn(account(1L, "xingye", "星野", "13800000001", 1));
        when(accountMapper.update(any(), any())).thenReturn(1);

        service.save(form(1L, " 星野新 ", "13800000002", 0));

        verify(accountMapper).update(any(), any());
        // 编辑不碰密码，密码只能走重置入口
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void 保存值未变化时不算失败() {
        when(accountMapper.selectById(1L)).thenReturn(account(1L, "xingye", "星野", "13800000001", 1));
        // MySQL 对“值没变”的 UPDATE 返回 0 行，不能当成用户不存在
        when(accountMapper.update(any(), any())).thenReturn(0);

        service.save(form(1L, "  ", "", 1));

        verify(accountMapper).update(any(), any());
    }

    @Test
    void 保存状态越界返回四百() {
        when(accountMapper.selectById(1L)).thenReturn(account(1L, "xingye", "星野", "13800000001", 1));

        BizException exception = assertThrows(BizException.class,
                () -> service.save(form(1L, "星野", "13800000001", 5)));

        assertTrue(exception.getMessage().contains("账号状态"));
        verify(accountMapper, never()).update(any(), any());
    }

    @Test
    void 保存手机号非法返回四百() {
        when(accountMapper.selectById(1L)).thenReturn(account(1L, "xingye", "星野", "13800000001", 1));

        BizException exception = assertThrows(BizException.class,
                () -> service.save(form(1L, "星野", "123", 1)));

        assertTrue(exception.getMessage().contains("手机号"));
        verify(accountMapper, never()).update(any(), any());
    }

    @Test
    void 保存用户不存在返回四百() {
        when(accountMapper.selectById(9L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class,
                () -> service.save(form(9L, "星野", "13800000001", 1)));

        assertEquals("用户不存在或已被删除", exception.getMessage());
        verify(accountMapper, never()).update(any(), any());
    }

    // ------------------------------------------------------------ 重置密码

    @Test
    void 重置密码写成BCrypt密文() {
        when(accountMapper.selectById(1L)).thenReturn(account(1L, "xingye", "星野", "13800000001", 1));
        when(passwordEncoder.encode("abc123")).thenReturn("$2a$10$mocked-hash");
        when(accountMapper.update(any(), any())).thenReturn(1);

        service.resetPassword(1L, "abc123");

        verify(passwordEncoder).encode("abc123");
        verify(accountMapper).update(any(), any());
    }

    @Test
    void 重置密码强度不足返回四百() {
        when(accountMapper.selectById(1L)).thenReturn(account(1L, "xingye", "星野", "13800000001", 1));

        BizException exception = assertThrows(BizException.class, () -> service.resetPassword(1L, "123"));

        assertTrue(exception.getMessage().contains("6–20"));
        verifyNoInteractions(passwordEncoder);
        verify(accountMapper, never()).update(any(), any());
    }

    @Test
    void 重置密码用户不存在返回四百() {
        when(accountMapper.selectById(9L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> service.resetPassword(9L, "abc123"));

        assertEquals("用户不存在或已被删除", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    // ------------------------------------------------------------ 夹具

    private AdminAccountForm form(Long id, String nickname, String phone, Integer status) {
        AdminAccountForm form = new AdminAccountForm();
        form.setId(id);
        form.setNickname(nickname);
        form.setPhone(phone);
        form.setStatus(status);
        return form;
    }

    private Account account(Long id, String username, String nickname, String phone, Integer status) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setNickname(nickname);
        account.setPhone(phone);
        account.setStatus(status);
        account.setCreateTime(LocalDateTime.of(2026, 11, 17, 20, 15, 1));
        account.setLastLoginTime(LocalDateTime.of(2026, 11, 17, 20, 20, 0));
        return account;
    }
}
