package com.ipc.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.dto.AdminProductForm;
import com.ipc.mall.dto.SpecPair;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Product;
import com.ipc.mall.vo.AdminProductRowVO;

import java.util.List;
import java.util.Optional;

/**
 * 后台商品管理（PRD §6.3 / docs/api.md A9–A13）。
 * <p>
 * SPU 与它的 SKU 子表一起保存（A11）；删除前校验未完结订单（2005）。
 */
public interface ProductAdminService {

    /** A9：分页列表；{@code categoryIds} 为 null 表示不按分类过滤 */
    IPage<AdminProductRowVO> page(String keyword, List<Long> categoryIds, Integer status, int pageNo, int size);

    /** 表单回填 */
    Optional<Product> get(Long id);

    /** 该 SPU 下的 SKU（含库存/版本，表单与 SKU 页共用） */
    List<Item> itemsOf(Long productId);

    /** product.specs JSON → 有序 key-value，表单参数表回填用 */
    List<SpecPair> specPairsOf(Product product);

    /** product.images 逗号串 → 相册列表 */
    List<String> galleryOf(Product product);

    /** A11：新增 / 编辑 SPU 与 SKU 子表（同一事务） */
    void save(AdminProductForm form);

    /** A12：上下架，只接受 0/1 */
    void updateStatus(Long id, Integer status);

    /** A13：逻辑删除 SPU 及其 SKU；有未完结订单时 2005 */
    void remove(Long id);
}
