package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ipc.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品 SKU（PRD §7.3 item）。
 * <p>
 * 价格、库存、乐观锁都在这一层：加购和下单都针对 itemId，而不是 productId。
 * 库存扣减用 {@code UPDATE item SET stock = stock - ? WHERE id = ? AND stock >= ?}
 * 或带 version 的乐观锁更新，二选一，不要混用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("item")
public class Item extends BaseEntity {

    private Long productId;

    /** 规格名，如「500ml 星白」，同一 SPU 内唯一 */
    private String spec;

    private BigDecimal price;

    /** 可销售库存 */
    private Integer stock;

    /** 规格图，空则用 SPU 主图 */
    private String image;

    /**
     * 乐观锁版本号，更新失败（影响行数 0）说明期间库存被并发改动，
     * 应提示"库存已变化，请刷新重试"。
     */
    @Version
    private Integer version;
}
