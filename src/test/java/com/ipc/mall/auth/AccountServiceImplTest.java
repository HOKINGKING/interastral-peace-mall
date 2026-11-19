package com.ipc.mall.auth;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.ChangePasswordForm;
import com.ipc.mall.dto.RegisterForm;
import com.ipc.mall.entity.Account;
import com.ipc.mall.mapper.AccountMapper;
import com.ipc.mall.service.impl.AccountServiceImpl;
import com.ipc.mall.vo.LoginAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 注册/登录/改密业务规则（PRD §5.5–5.8）。
 */
class AccountServiceImplTest {

    private AccountMapper accountMapper;
    private BCryptPasswordEncoder passwordEncoder;
    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountMapper = mock(AccountMapper.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        accountService = new AccountServiceImpl(accountMapper, passwordEncoder);
    }

    @Test
    void 注册重名返回一千零一() {
        when(accountMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BizException exception = assertThrows(BizException.class, () -> accountService.register(form("xingye")));
        assertEquals(ResultCode.USERNAME_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void 注册写入BCrypt密文与默认昵称() {
        when(accountMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(accountMapper.insert(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(10L);
            return 1;
        });

        LoginAccount loginAccount = accountService.register(form("new_user"));

        assertEquals(10L, loginAccount.getAccountId());
        assertEquals("new_user", loginAccount.getNickname());
        verify(accountMapper).insert(any(Account.class));
    }

    @Test
    void 密码错误统一返回一千零二() {
        Account account = account(1L, "xingye", "PetStore123!", AccountStatus.ENABLED);
        when(accountMapper.selectOne(any(Wrapper.class))).thenReturn(account);

        BizException exception = assertThrows(BizException.class,
                () -> accountService.login("xingye", "wrong-password"));
        assertEquals(ResultCode.LOGIN_FAILED.getCode(), exception.getCode());
    }

    @Test
    void 禁用账号返回一千零四() {
        Account account = account(3L, "disabled", "PetStore123!", AccountStatus.DISABLED);
        when(accountMapper.selectOne(any(Wrapper.class))).thenReturn(account);

        BizException exception = assertThrows(BizException.class,
                () -> accountService.login("disabled", "PetStore123!"));
        assertEquals(ResultCode.ACCOUNT_DISABLED.getCode(), exception.getCode());
    }

    @Test
    void 登录成功更新最后登录时间() {
        Account account = account(1L, "xingye", "PetStore123!", AccountStatus.ENABLED);
        when(accountMapper.selectOne(any(Wrapper.class))).thenReturn(account);

        LoginAccount loginAccount = accountService.login("xingye", "PetStore123!");

        assertEquals(1L, loginAccount.getAccountId());
        assertEquals("星野", loginAccount.getDisplayName());
        verify(accountMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void 原密码错误返回一千零五() {
        Account account = account(1L, "xingye", "PetStore123!", AccountStatus.ENABLED);
        when(accountMapper.selectById(1L)).thenReturn(account);

        ChangePasswordForm form = new ChangePasswordForm();
        form.setOldPassword("wrong");
        form.setNewPassword("NewPass123");
        form.setConfirmPassword("NewPass123");

        BizException exception = assertThrows(BizException.class,
                () -> accountService.changePassword(1L, form));
        assertEquals(ResultCode.OLD_PASSWORD_ERROR.getCode(), exception.getCode());
    }

    @Test
    void 注册密码不是明文入库() {
        when(accountMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(accountMapper.insert(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            assertNotEquals("Passw0rd", account.getPassword());
            assertTrue(passwordEncoder.matches("Passw0rd", account.getPassword()));
            account.setId(1L);
            return 1;
        });

        accountService.register(form("new_user"));
    }

    private RegisterForm form(String username) {
        RegisterForm form = new RegisterForm();
        form.setUsername(username);
        form.setPassword("Passw0rd");
        form.setConfirmPassword("Passw0rd");
        form.setPhone("");
        form.setCaptcha("ABCD");
        return form;
    }

    private Account account(Long id, String username, String rawPassword, int status) {
        Account account = new Account();
        account.setId(id);
        account.setUsername(username);
        account.setNickname("星野");
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setStatus(status);
        return account;
    }
}
