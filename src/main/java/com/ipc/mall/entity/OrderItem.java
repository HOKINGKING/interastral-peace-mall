package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细，商品信息全快照（PRD §7.3 order_item）。
 * <p>
 * <b>不继承 BaseEntity</b>：这张表没有 update_time 和 deleted，
 * 明细随主单走（主单逻辑删除即可），也没有独立的更新语义。
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 原 SKU id，仅用于追溯，展示时不要回查商品（商品可能已改价/下架） */
    private Long itemId;

    /** 原 SPU id 快照：销量归集与统计用（PRD §7.6），不参与展示 */
    private Long productId;

    /** 下单时的商品名快照 */
    private String productName;

    /** 下单时的规格快照 */
    private String spec;

    /** 下单时单价 */
    private BigDecimal price;

    private Integer quantity;

    /** 下单时的图片快照，订单列表展示用 */
    private String image;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 行小计，数据库无该列，仅供页面计算使用 */
    public BigDecimal lineAmount() {
        return price == null || quantity == null
                ? BigDecimal.ZERO
                : price.multiply(BigDecimal.valueOf(quantity));
    }
}
