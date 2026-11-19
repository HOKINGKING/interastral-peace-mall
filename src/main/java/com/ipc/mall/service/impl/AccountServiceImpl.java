package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.ChangePasswordForm;
import com.ipc.mall.dto.RegisterForm;
import com.ipc.mall.entity.Account;
import com.ipc.mall.mapper.AccountMapper;
import com.ipc.mall.service.AccountService;
import com.ipc.mall.vo.AccountProfileVO;
import com.ipc.mall.vo.LoginAccount;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 前台账号认证实现（PRD §5.5–5.8）。
 */
@Service
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountMapper accountMapper, PasswordEncoder passwordEncoder) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginAccount register(RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BizException(ResultCode.FAIL.getCode(), "两次输入的密码不一致");
        }

        String username = form.getUsername().trim();
        Long exists = accountMapper.selectCount(
                Wrappers.<Account>query().eq("username", username));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(form.getPassword()));
        account.setNickname(username);
        account.setPhone(normalizePhone(form.getPhone()));
        account.setStatus(AccountStatus.ENABLED);
        account.setLastLoginTime(LocalDateTime.now());

        try {
            accountMapper.insert(account);
        } catch (DuplicateKeyException e) {
            // 并发注册时查重可能同时通过，由 (username, deleted) 唯一索引兜底
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }
        return LoginAccount.of(account);
    }

    @Override
    public LoginAccount login(String username, String password) {
        String loginName = username == null ? "" : username.trim();
        Account account = accountMapper.selectOne(
                Wrappers.<Account>query().eq("username", loginName));

        // 用户不存在与密码错误统一返回 1002，避免用户名枚举
        if (account == null || password == null
                || !passwordEncoder.matches(password, account.getPassword())) {
            throw new BizException(ResultCode.LOGIN_FAILED);
        }
        if (!AccountStatus.isEnabled(account.getStatus())) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }

        accountMapper.update(null, Wrappers.<Account>update()
                .eq("id", account.getId())
                .set("last_login_time", LocalDateTime.now()));
        return LoginAccount.of(account);
    }

    @Override
    public AccountProfileVO getProfile(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return AccountProfileVO.of(account);
    }

    @Override
    public void changePassword(Long accountId, ChangePasswordForm form) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(form.getOldPassword(), account.getPassword())) {
            throw new BizException(ResultCode.OLD_PASSWORD_ERROR);
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BizException(ResultCode.FAIL.getCode(), "两次输入的新密码不一致");
        }

        accountMapper.update(null, Wrappers.<Account>update()
                .eq("id", accountId)
                .set("password", passwordEncoder.encode(form.getNewPassword())));
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String value = phone.trim();
        return value.isEmpty() ? null : value;
    }
}
