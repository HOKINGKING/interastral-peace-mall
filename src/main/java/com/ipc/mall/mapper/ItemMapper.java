package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.Item;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/** 商品 SKU Mapper */
public interface ItemMapper extends BaseMapper<Item> {

    /**
     * 下单扣库存（PRD §5.11 第 4 条）：带乐观锁版本与库存下限，影响行数为 0 即视为被抢购。
     * <p>
     * <b>不要自动重试</b>：返回 2002 让用户刷新重试，才能保证同一 SKU 不超卖。
     */
    @Update("UPDATE item SET stock = stock - #{qty}, version = version + 1 "
            + "WHERE id = #{id} AND stock >= #{qty} AND version = #{version}")
    int deductStock(@Param("id") Long id, @Param("qty") Integer qty, @Param("version") Integer version);

    /**
     * 取消订单回补库存（T3）：配合「状态条件更新成功」使用，保证只回补一次。
     * 这里不带 version 条件：回补是加库存，不存在超卖问题。
     */
    @Update("UPDATE item SET stock = stock + #{qty}, version = version + 1 WHERE id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("qty") Integer qty);

    /**
     * 后台编辑 SKU：整行覆盖 + 乐观锁（PRD §6.3「改库存需校验 version 一致」）。
     * 影响行数 0 = 已被并发修改或不属于该 SPU 或已删除。
     */
    @Update("UPDATE item SET spec = #{i.spec}, price = #{i.price}, stock = #{i.stock}, image = #{i.image}, "
            + "version = version + 1, update_time = #{updateTime} "
            + "WHERE id = #{i.id} AND product_id = #{i.productId} AND version = #{i.version} AND deleted = 0")
    int updateSkuWithVersion(@Param("i") Item item, @Param("updateTime") LocalDateTime updateTime);

    /** 后台删除 SPU 时一并逻辑删除它的 SKU，避免孤儿行在别处出现 */
    @Update("UPDATE item SET deleted = 1, update_time = #{updateTime} "
            + "WHERE product_id = #{productId} AND deleted = 0")
    int logicDeleteByProduct(@Param("productId") Long productId, @Param("updateTime") LocalDateTime updateTime);
}
