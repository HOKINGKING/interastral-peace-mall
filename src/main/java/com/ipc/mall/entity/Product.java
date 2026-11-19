package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ipc.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品 SPU（PRD §7.3 product）。
 * <p>
 * 注意这一层<b>没有价格和库存</b>：它们属于 {@link Item}（SKU）。
 * 列表页展示价取该 SPU 下最低价 SKU，详情页按 SKU 切换。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product")
public class Product extends BaseEntity {

    /** 必须挂二级分类，由 Service 校验 */
    private Long categoryId;

    private String name;

    private String brand;

    /** 短描述，列表卡片与详情页顶部 */
    private String descn;

    /** 详情正文，模板按纯文本转义展示 */
    private String detail;

    /** 主图 URL，空则前端用占位图 */
    private String mainImage;

    /** 相册：逗号分隔的图片 URL，≤5 张，首张建议与主图一致（PRD §5.4） */
    private String images;

    /** 商品参数：JSON 对象，≤8 组 key-value（PRD §5.4） */
    private String specs;

    /** 累计已售件数：支付成功时按订单明细累加，取消不回退（PRD §7.6） */
    private Integer sales;

    /** 1 上架 / 0 下架，见 ProductStatus */
    private Integer status;
}
