package com.ipc.mall.service;

import com.ipc.mall.dto.AdminSkuForm;
import com.ipc.mall.entity.Item;

import java.util.List;
import java.util.Optional;

/**
 * 后台 SKU 管理（PRD §6.3 / docs/api.md A14–A16）。
 * <p>
 * 编辑时校验同 SPU 内规格唯一、价格/库存范围，并用 {@code version} 做乐观锁；
 * 每个 SPU 至少保留 1 条 SKU。
 */
public interface ItemAdminService {

    /** A14：某 SPU 下的 SKU 列表 */
    List<Item> listByProduct(Long productId);

    Optional<Item> get(Long id);

    /** A15：新增 / 编辑（编辑必须带 version） */
    void save(AdminSkuForm form);

    /** A16：逻辑删除；最后一条 SKU 禁删 */
    void remove(Long id);
}
