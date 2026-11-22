package com.ipc.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.dto.ProductQuery;
import com.ipc.mall.vo.ProductCardVO;
import com.ipc.mall.vo.ProductDetailVO;
import com.ipc.mall.vo.SkuVO;

import java.util.List;
import java.util.Optional;

/**
 * 前台商品读取（PRD §5.1–§5.4）。
 * <p>
 * 写操作（上下架、SKU 维护）属于后台模块，这里只做展示相关查询。
 */
public interface ProductService {

    /** 首页推荐：上架商品按更新时间倒序取 limit 条 */
    List<ProductCardVO> recommended(int limit);

    /** 列表 / 分类页 / 搜索的分页查询 */
    IPage<ProductCardVO> pageCards(ProductQuery query, List<Long> categoryIds);

    /** 详情聚合：SKU、相册、参数、面包屑、看了又看；商品不存在返回 empty */
    Optional<ProductDetailVO> detail(Long productId);

    /** 详情页规格切换 / GET /product/{id}/skus */
    List<SkuVO> skusOf(Long productId);

    boolean exists(Long productId);
}
