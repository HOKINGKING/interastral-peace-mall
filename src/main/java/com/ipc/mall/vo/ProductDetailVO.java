package com.ipc.mall.vo;

import com.ipc.mall.entity.Category;
import com.ipc.mall.entity.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品详情聚合：SPU + SKU 列表 + 相册 + 参数 + 面包屑 + 推荐。
 */
@Data
public class ProductDetailVO {

    private Product product;

    private List<SkuVO> skus;

    /** 相册图片 URL 列表（≤5，至少含主图；空表示用占位图） */
    private List<String> images;

    /** 商品参数（有序），JSON 非法时为空 Map */
    private Map<String, String> specs;

    /** 商品所属二级分类 */
    private Category category;

    /** 二级分类的上级（一级），可能为空 */
    private Category parentCategory;

    /** 最低价，列表价与详情初始价一致 */
    private BigDecimal minPrice;

    /** 总库存 */
    private int totalStock;

    /** 全部 SKU 售罄 */
    private boolean soldOut;

    /** SPU 已下架（详情仍可访问，但不可加购） */
    private boolean offSale;

    /** 默认选中的 SKU：第一个有库存的，全部售罄时取第一个 */
    private SkuVO defaultSku;

    /** 默认 SKU 的数量上限：min(stock, 99)，售罄为 0 */
    private int maxQty;

    /** 看了又看 */
    private List<ProductCardVO> related;
}
