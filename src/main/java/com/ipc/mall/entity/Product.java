package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ipc.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品实体，同时作为<b>实体层写法样板</b>，其余 7 个实体照抄以下约定：
 * <ol>
 *     <li>继承 {@link BaseEntity}，拿到 id / createdAt / updatedAt，不重复声明；</li>
 *     <li>{@code @TableName} 显式写表名（sales_order、store_user 这类表名与类名不一致时必须写）；</li>
 *     <li>状态字段用 {@code String}，配合 ProductStatus / OrderStatus 常量；</li>
 *     <li>需要防旧值覆盖的列加 {@code @Version}（由 MybatisPlusConfig 里的乐观锁插件生效）；</li>
 *     <li>金额一律 BigDecimal，绝不用 double；</li>
 *     <li>子类加 {@code @EqualsAndHashCode(callSuper = true)}，否则 Lombok 会警告并漏掉父类字段。</li>
 * </ol>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {

    /** 唯一销售编码，创建后不修改 */
    private String sku;

    /** 归属二级分类 id */
    private Long categoryId;

    private String name;

    /** 副标题/卖点，列表页展示 */
    private String summary;

    /** 纯文本详情，模板中需转义展示 */
    private String description;

    /** 封面图，站内静态资源相对路径，如 images/products/placeholder-1.jpg */
    private String coverImage;

    /** 售价，DECIMAL(12,2) */
    private BigDecimal price;

    /** 可销售库存：下单扣减，取消未支付订单时恢复 */
    private Integer stock;

    /** DRAFT / ON_SALE，见 {@link com.ipc.mall.common.constant.ProductStatus} */
    private String status;

    /** 排序值，越大越靠前 */
    private Integer sortOrder;

    /**
     * 乐观锁版本号。
     * 更新时必须带上读取时的 version，MyBatis-Plus 会生成
     * {@code UPDATE product SET ..., version = version + 1 WHERE id = ? AND version = ?}，
     * 影响行数为 0 说明期间被别人改过，业务层应抛出"库存或价格已变更，请刷新重试"。
     */
    @Version
    private Long version;
}
