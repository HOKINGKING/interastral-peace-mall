package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.Cart;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 购物车 Mapper。
 * <p>
 * 这里的原生 SQL 都绕开了 MyBatis-Plus 的逻辑删除自动拼装：
 * MP 会给 update/select 自动追加 {@code deleted = 0}，改不到、也查不到已逻辑删除的行。
 * 购物车的「复活」正好需要绕过它，所以用显式 SQL + {@code id + account_id} 条件更新，
 * 影响行数为 0 即视为越权 / 行不存在（PRD §7.5、handoff §8.2）。
 */
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

    /**
     * 加购的「累加 / 复活」：一条条件更新同时覆盖两种情形。
     * <ul>
     *     <li>行已存在（deleted=0）：数量直接覆盖为累加后的新值</li>
     *     <li>行已逻辑删除（deleted=1）：deleted 置 0 + 数量覆盖，即「复活」，绝不 insert 新行</li>
     * </ul>
     * 显式写 update_time：Wrapper 更新不会触发 {@code MyMetaObjectHandler} 的 updateFill。
     */
    @Update("UPDATE cart SET quantity = #{quantity}, deleted = 0, update_time = #{updateTime} "
            + "WHERE id = #{id} AND account_id = #{accountId}")
    int updateQuantityAndRevive(@Param("id") Long id,
                                @Param("accountId") Long accountId,
                                @Param("quantity") Integer quantity,
                                @Param("updateTime") LocalDateTime updateTime);

    /** 修改数量：条件更新只动自己的行，影响行数 0 = 越权或行已删除 */
    @Update("UPDATE cart SET quantity = #{quantity}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND account_id = #{accountId} AND deleted = 0")
    int updateQuantityOwned(@Param("id") Long id,
                            @Param("accountId") Long accountId,
                            @Param("quantity") Integer quantity,
                            @Param("updateTime") LocalDateTime updateTime);

    /** 删除：逻辑删除 + 归属校验合成一条 SQL，避免「先查后删」的竞态 */
    @Update("UPDATE cart SET deleted = 1, update_time = #{updateTime} "
            + "WHERE id = #{id} AND account_id = #{accountId} AND deleted = 0")
    int logicDeleteOwned(@Param("id") Long id,
                         @Param("accountId") Long accountId,
                         @Param("updateTime") LocalDateTime updateTime);

    /** 顶部角标：该用户购物车内商品总件数（数量求和，不含已删除行） */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM cart WHERE account_id = #{accountId} AND deleted = 0")
    int sumQuantityByAccount(@Param("accountId") Long accountId);
}
