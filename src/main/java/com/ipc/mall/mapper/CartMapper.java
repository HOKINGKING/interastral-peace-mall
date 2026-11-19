package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.Cart;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 购物车 Mapper */
public interface CartMapper extends BaseMapper<Cart> {

    /**
     * 按 (account_id, item_id) 查询<b>包含已逻辑删除</b>的记录（PRD §7.5 第 2 个坑）。
     * <p>
     * 必须用原生 SQL：BaseMapper 的查询会被自动追加 {@code deleted = 0}，
     * 查不到已删记录，加购时就会插出重复行导致合计错误。
     * 查到记录后由 Service 把它 deleted 置 0 并累加数量（"复活"）。
     */
    @Select("SELECT * FROM cart WHERE account_id = #{accountId} AND item_id = #{itemId} LIMIT 1")
    Cart selectIncludingDeleted(@Param("accountId") Long accountId, @Param("itemId") Long itemId);
}
