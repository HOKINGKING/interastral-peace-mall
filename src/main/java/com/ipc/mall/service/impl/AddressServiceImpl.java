package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.AddressForm;
import com.ipc.mall.entity.Address;
import com.ipc.mall.mapper.AddressMapper;
import com.ipc.mall.service.AddressService;
import com.ipc.mall.vo.AddressVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * 收货地址实现（PRD §5.10 / docs/api.md F19–F23）。
 * <p>
 * 两条铁律：
 * <ol>
 *     <li>每个按 id 的写操作都必须带 {@code account_id} 条件，越权一律 3004；</li>
 *     <li>「首个地址自动默认」与「设默认清其他」放在同一个事务里，
 *         保证库里任何时刻最多一条 {@code is_default = 1}。</li>
 * </ol>
 * 另外两条容易写错的规则：上限 20 只在<b>新增</b>时校验；删除默认地址后<b>不补选</b>。
 */
@Slf4j
@Service
public class AddressServiceImpl implements AddressService {

    private static final String NOT_MINE = "收货地址不存在或不属于当前用户";
    private static final String LIMIT_EXCEEDED = "收货地址最多保存 20 条";

    private final AddressMapper addressMapper;

    public AddressServiceImpl(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    // ------------------------------------------------------------------ 读

    @Override
    public List<AddressVO> list(Long accountId) {
        if (accountId == null) {
            return List.of();
        }
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                        .eq(Address::getAccountId, accountId)
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getUpdateTime)
                        .orderByDesc(Address::getId))
                .stream()
                .map(AddressVO::of)
                .toList();
    }

    @Override
    public Optional<Address> get(Long accountId, Long id) {
        if (accountId == null || id == null) {
            return Optional.empty();
        }
        Address address = addressMapper.selectById(id);
        if (address == null || !Objects.equals(address.getAccountId(), accountId)) {
            return Optional.empty();
        }
        return Optional.of(address);
    }

    @Override
    public int count(Long accountId) {
        if (accountId == null) {
            return 0;
        }
        Long total = addressMapper.selectCount(new LambdaQueryWrapper<Address>()
                .eq(Address::getAccountId, accountId));
        return total == null ? 0 : total.intValue();
    }

    // ------------------------------------------------------------------ 写

    @Override
    @Transactional
    public void save(Long accountId, AddressForm form) {
        if (accountId == null || form == null) {
            throw new BizException(ResultCode.NO_PERMISSION.getCode(), NOT_MINE);
        }
        normalize(form);
        boolean wantDefault = parseDefault(form.getIsDefault());
        LocalDateTime now = LocalDateTime.now();

        if (form.getId() == null) {
            int total = count(accountId);
            // 上限只挡新增，编辑已有地址不受影响
            if (total >= MAX_COUNT) {
                throw new BizException(ResultCode.FAIL.getCode(), LIMIT_EXCEEDED);
            }
            // 首个地址自动默认，忽略前端传值
            boolean defaultFlag = total == 0 || wantDefault;
            if (defaultFlag) {
                addressMapper.clearDefault(accountId, now);
            }
            Address address = new Address();
            address.setAccountId(accountId);
            applyFields(address, form);
            address.setIsDefault(defaultFlag ? 1 : 0);
            addressMapper.insert(address);
            return;
        }

        Address existing = addressMapper.selectById(form.getId());
        if (existing == null || !Objects.equals(existing.getAccountId(), accountId)) {
            throw new BizException(ResultCode.NO_PERMISSION.getCode(), NOT_MINE);
        }
        if (wantDefault) {
            addressMapper.clearDefault(accountId, now);
        }

        // 整行覆盖 + 归属校验一条 SQL 完成；省市区允许被改回空，写入 NULL 由这条 SQL 负责
        Address update = new Address();
        update.setId(form.getId());
        update.setAccountId(accountId);
        applyFields(update, form);
        update.setIsDefault(wantDefault ? 1 : 0);
        update.setUpdateTime(now);
        int rows = addressMapper.updateOwned(update);
        if (rows == 0) {
            throw new BizException(ResultCode.NO_PERMISSION.getCode(), NOT_MINE);
        }
        log.debug("地址更新: account={} id={} default={}", accountId, form.getId(), wantDefault);
    }

    @Override
    @Transactional
    public void remove(Long accountId, Long id) {
        if (get(accountId, id).isEmpty()) {
            throw new BizException(ResultCode.NO_PERMISSION.getCode(), NOT_MINE);
        }
        // 逻辑删除（MP 全局配置），删除默认地址后不自动补选其它地址
        addressMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long accountId, Long id) {
        if (get(accountId, id).isEmpty()) {
            throw new BizException(ResultCode.NO_PERMISSION.getCode(), NOT_MINE);
        }
        LocalDateTime now = LocalDateTime.now();
        // 同一事务：先清该用户其它默认，再把目标置 1
        addressMapper.clearDefault(accountId, now);
        int rows = addressMapper.markDefault(id, accountId, now);
        if (rows == 0) {
            throw new BizException(ResultCode.NO_PERMISSION.getCode(), NOT_MINE);
        }
    }

    // ------------------------------------------------------------------ 内部方法

    private void applyFields(Address address, AddressForm form) {
        address.setReceiver(form.getReceiver());
        address.setPhone(form.getPhone());
        address.setProvince(form.getProvince());
        address.setCity(form.getCity());
        address.setDistrict(form.getDistrict());
        address.setDetail(form.getDetail());
    }

    /** 去掉首尾空白；省市区空串归一化成 null，避免库里出现 "" */
    private void normalize(AddressForm form) {
        form.setReceiver(trim(form.getReceiver()));
        form.setPhone(trim(form.getPhone()));
        form.setProvince(blankToNull(form.getProvince()));
        form.setCity(blankToNull(form.getCity()));
        form.setDistrict(blankToNull(form.getDistrict()));
        form.setDetail(trim(form.getDetail()));
    }

    /** checkbox 的 "on"、隐藏域的 "1"、null、"" 统一成 true/false */
    private boolean parseDefault(String raw) {
        if (raw == null) {
            return false;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "1".equals(value) || "true".equals(value) || "on".equals(value) || "yes".equals(value);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
