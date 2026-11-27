package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.AdminAccountForm;
import com.ipc.mall.entity.Account;
import com.ipc.mall.entity.Order;
import com.ipc.mall.mapper.AccountMapper;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.service.AccountAdminService;
import com.ipc.mall.vo.AdminAccountDetailVO;
import com.ipc.mall.vo.AdminAccountRowVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 后台用户管理实现（PRD §6.4）。
 * <p>
 * 三条约束：
 * <ol>
 *     <li>只改昵称 / 手机号 / 状态，用户名与密码不在编辑表单里；</li>
 *     <li>手机号留空表示解绑，写入 NULL 而不是空串；</li>
 *     <li>禁用不删数据：登录直接拒绝，已登录会话由拦截器每 5 分钟回查踢下线。</li>
 * </ol>
 */
@Slf4j
@Service
public class AccountAdminServiceImpl implements AccountAdminService {

    /** 手机号：1 开头 11 位数字，与注册/地址表单保持一致 */
    private static final String PHONE_REGEX = "^1\\d{10}$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{6,20}$";

    private final AccountMapper accountMapper;
    private final OrderMapper orderMapper;
    private final PasswordEncoder passwordEncoder;

    public AccountAdminServiceImpl(AccountMapper accountMapper,
                                   OrderMapper orderMapper,
                                   PasswordEncoder passwordEncoder) {
        this.accountMapper = accountMapper;
        this.orderMapper = orderMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------------------------------------------------------ 列表 / 详情

    @Override
    public IPage<AdminAccountRowVO> page(String keyword, Integer status, int pageNo, int size) {
        Page<Account> request = new Page<>(Math.max(1, pageNo), Math.max(1, size));
        String needle = trimToNull(keyword);
        Integer statusFilter = normalizeStatus(status);

        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        if (needle != null) {
            // 用户名 / 昵称 / 手机号三选一命中；手机号也走 LIKE，方便按尾号搜索
            wrapper.and(w -> w.like(Account::getUsername, needle)
                    .or().like(Account::getNickname, needle)
                    .or().like(Account::getPhone, needle));
        }
        if (statusFilter != null) {
            wrapper.eq(Account::getStatus, statusFilter);
        }
        wrapper.orderByDesc(Account::getCreateTime).orderByDesc(Account::getId);

        IPage<Account> raw = accountMapper.selectPage(request, wrapper);
        Page<AdminAccountRowVO> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(raw.getRecords().stream().map(AdminAccountRowVO::of).toList());
        return result;
    }

    @Override
    public AdminAccountDetailVO detail(Long id) {
        Account account = requireAccount(id);
        // 逻辑删除的订单不计入；selectCount 已由 MP 自动追加 deleted = 0
        Long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getAccountId, account.getId()));
        return AdminAccountDetailVO.of(account, orderCount == null ? 0L : orderCount);
    }

    // ------------------------------------------------------------------ 写操作

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(AdminAccountForm form) {
        if (form == null || form.getId() == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "缺少用户 id");
        }
        Account account = requireAccount(form.getId());

        Integer status = form.getStatus();
        if (status == null || (status != AccountStatus.ENABLED && status != AccountStatus.DISABLED)) {
            throw new BizException(ResultCode.FAIL.getCode(), "账号状态只能是 0 或 1");
        }
        String nickname = trimToNull(form.getNickname());
        if (nickname != null && nickname.length() > 20) {
            throw new BizException(ResultCode.FAIL.getCode(), "昵称最多 20 个字符");
        }
        String phone = trimToNull(form.getPhone());
        if (phone != null && !phone.matches(PHONE_REGEX)) {
            throw new BizException(ResultCode.FAIL.getCode(), "手机号需为 11 位数字");
        }

        // 显式 set：手机号允许被清空成 NULL，updateById 会跳过 null 字段所以不能用实体更新。
        // 这里用字符串列名（与 AccountServiceImpl 的写法一致），避免单元测试里没有 TableInfo 时解析失败
        int rows = accountMapper.update(null, new UpdateWrapper<Account>()
                .eq("id", account.getId())
                .set("nickname", nickname)
                .set("phone", phone)
                .set("status", status)
                .set("update_time", LocalDateTime.now()));
        if (rows == 0) {
            // 值未变化时 MySQL 也返回 0，这里只记日志，不能当成“用户不存在”
            log.debug("用户信息未发生变化: id={}", account.getId());
        }
        log.info("后台更新用户: id={} username={} status={}", account.getId(), account.getUsername(), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String password) {
        Account account = requireAccount(id);
        if (password == null || !password.matches(PASSWORD_REGEX)) {
            throw new BizException(ResultCode.FAIL.getCode(), "密码需为 6–20 位且同时包含字母和数字");
        }
        int rows = accountMapper.update(null, new UpdateWrapper<Account>()
                .eq("id", account.getId())
                .set("password", passwordEncoder.encode(password))
                .set("update_time", LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ResultCode.FAIL.getCode(), "用户不存在或已被删除");
        }
        // 只记账号标识，绝不把明文或密文写进日志
        log.info("后台重置用户密码: id={} username={}", account.getId(), account.getUsername());
    }

    // ------------------------------------------------------------------ 内部方法

    private Account requireAccount(Long id) {
        Account account = id == null ? null : accountMapper.selectById(id);
        if (account == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "用户不存在或已被删除");
        }
        return account;
    }

    /** 筛选条件只接受 0 / 1，其它值按“全部”处理 */
    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return null;
        }
        if (status == AccountStatus.ENABLED || status == AccountStatus.DISABLED) {
            return status;
        }
        log.warn("忽略非法的用户状态筛选: {}", status);
        return null;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
