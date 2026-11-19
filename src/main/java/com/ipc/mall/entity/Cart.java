package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ipc.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 购物车（PRD §7.3 cart）。
 * <p>
 * 注意 PRD §7.5 的第 2 个坑：<b>cart 不建唯一索引</b>。
 * 加购时按 (account_id, item_id) 查询<b>含已删除</b>的记录：
 * 存在则把 deleted 置 0 并累加数量（"复活"），否则新增一行。
 * 若直接用 BaseMapper.selectOne 会被自动追加 deleted = 0，查不到已删记录导致重复行。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cart")
public class Cart extends BaseEntity {

    private Long accountId;

    private Long itemId;

    /** 数量，1–99（数据库 CHECK 兜底） */
    private Integer quantity;
}
