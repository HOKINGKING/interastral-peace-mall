package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.Item;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
}
