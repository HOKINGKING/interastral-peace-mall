package com.ipc.mall.service;

import com.ipc.mall.dto.AddressForm;
import com.ipc.mall.entity.Address;
import com.ipc.mall.vo.AddressVO;

import java.util.List;
import java.util.Optional;

/**
 * 收货地址（PRD §5.10 / docs/api.md F19–F23）。
 * <p>
 * 所有方法都要求传入<b>会话里的</b> accountId；任何按 id 的操作都先校验归属，
 * 不一致抛 3004，绝不能只按主键改库。
 */
public interface AddressService {

    /** 地址数量上限（PRD §5.10：只统计未删除记录） */
    int MAX_COUNT = 20;

    /** 列表：默认地址最前，其次按更新时间倒序 */
    List<AddressVO> list(Long accountId);

    /** 编辑回填用；不存在或不属于该账号返回 empty（越权由调用方决定回跳还是 3004） */
    Optional<Address> get(Long accountId, Long id);

    /** 新增 / 编辑：首个地址自动默认，勾选默认时同事务清其他 */
    void save(Long accountId, AddressForm form);

    /** 逻辑删除；删除默认地址后不补选（PRD 明确要求） */
    void remove(Long accountId, Long id);

    /** 设为默认：同事务内先清该用户其它地址，再置目标为 1 */
    void setDefault(Long accountId, Long id);

    /** 当前有效地址条数 */
    int count(Long accountId);
}
