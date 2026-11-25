package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.AdminProductForm;
import com.ipc.mall.dto.AdminSkuForm;
import com.ipc.mall.dto.SpecPair;
import com.ipc.mall.entity.Category;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.CategoryMapper;
import com.ipc.mall.mapper.ItemMapper;
import com.ipc.mall.mapper.OrderItemMapper;
import com.ipc.mall.mapper.ProductMapper;
import com.ipc.mall.service.ProductAdminService;
import com.ipc.mall.vo.AdminProductRowVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 后台商品管理实现（PRD §6.3）。
 * <p>
 * 规则集中在这几处：
 * <ol>
 *     <li>SPU 必须挂在<b>二级分类</b>下；相册 ≤5 张、参数 ≤8 组；</li>
 *     <li>SKU 至少 1 条、同 SPU 内规格唯一、价格 0.01–99999.99、库存 0–999999；</li>
 *     <li>编辑 SKU 带 version 乐观锁，冲突提示刷新；表单里删掉的 SKU 走逻辑删除；</li>
 *     <li>删除前查未完结订单（PENDING_PAYMENT / PAID / SHIPPED）→ 2005。</li>
 * </ol>
 */
@Slf4j
@Service
public class ProductAdminServiceImpl implements ProductAdminService {

    private static final int IMAGE_MAX = 5;
    private static final int SPEC_MAX = 8;
    private static final BigDecimal PRICE_MIN = new BigDecimal("0.01");
    private static final BigDecimal PRICE_MAX = new BigDecimal("99999.99");
    private static final int STOCK_MAX = 999999;
    private static final int STATUS_ON = 1;
    private static final int STATUS_OFF = 0;

    private final ProductMapper productMapper;
    private final ItemMapper itemMapper;
    private final CategoryMapper categoryMapper;
    private final OrderItemMapper orderItemMapper;
    private final ObjectMapper objectMapper;

    public ProductAdminServiceImpl(ProductMapper productMapper,
                                   ItemMapper itemMapper,
                                   CategoryMapper categoryMapper,
                                   OrderItemMapper orderItemMapper,
                                   ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.itemMapper = itemMapper;
        this.categoryMapper = categoryMapper;
        this.orderItemMapper = orderItemMapper;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ 读

    @Override
    public IPage<AdminProductRowVO> page(String keyword, List<Long> categoryIds, Integer status,
                                        int pageNo, int size) {
        Page<AdminProductRowVO> page = new Page<>(Math.max(1, pageNo), Math.max(1, size));
        return productMapper.selectAdminPage(page, trimToNull(keyword), categoryIds, status);
    }

    @Override
    public Optional<Product> get(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(productMapper.selectById(id));
    }

    @Override
    public List<Item> itemsOf(Long productId) {
        if (productId == null) {
            return List.of();
        }
        return itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getProductId, productId)
                .orderByAsc(Item::getId));
    }

    @Override
    public List<SpecPair> specPairsOf(Product product) {
        if (product == null || !StringUtils.hasText(product.getSpecs())) {
            return List.of();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(product.getSpecs(),
                    new TypeReference<Map<String, Object>>() {
                    });
            List<SpecPair> pairs = new ArrayList<>();
            raw.forEach((key, value) -> pairs.add(new SpecPair(key, value == null ? "" : String.valueOf(value))));
            return pairs;
        } catch (Exception e) {
            // 参数 JSON 坏了不影响后台能打开表单，清空让卖家重填
            log.warn("商品参数 JSON 解析失败，已忽略: productId={}", product.getId());
            return List.of();
        }
    }

    @Override
    public List<String> galleryOf(Product product) {
        if (product == null || !StringUtils.hasText(product.getImages())) {
            return List.of();
        }
        List<String> images = new ArrayList<>();
        for (String part : product.getImages().split(",")) {
            String url = part.trim();
            if (!url.isEmpty() && !images.contains(url)) {
                images.add(url);
            }
        }
        return images;
    }

    // ------------------------------------------------------------------ 写

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(AdminProductForm form) {
        Category category = form.getCategoryId() == null ? null : categoryMapper.selectById(form.getCategoryId());
        if (category == null || category.isRoot()) {
            throw new BizException(ResultCode.FAIL.getCode(), "所属分类必须是二级分类");
        }

        List<String> images = splitImages(form.getImages());
        if (images.size() > IMAGE_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(), "相册最多 5 张");
        }
        Map<String, String> specs = normalizeSpecs(form.getSpecs());
        validateSkus(form.getSkus());

        int status = form.getStatus() != null && form.getStatus() == STATUS_ON ? STATUS_ON : STATUS_OFF;
        Product product = new Product();
        product.setCategoryId(category.getId());
        product.setName(form.getName().trim());
        product.setBrand(blankToNull(form.getBrand()));
        product.setDescn(blankToNull(form.getDescn()));
        product.setDetail(blankToNull(form.getDetail()));
        product.setMainImage(blankToNull(form.getMainImage()));
        product.setImages(images.isEmpty() ? null : String.join(",", images));
        product.setSpecs(specs.isEmpty() ? null : toJson(specs));
        product.setStatus(status);

        LocalDateTime now = LocalDateTime.now();
        if (form.getId() == null) {
            productMapper.insert(product);
            log.info("新增商品: id={} name={}", product.getId(), product.getName());
        } else {
            Product existing = productMapper.selectById(form.getId());
            if (existing == null) {
                throw new BizException(ResultCode.FAIL.getCode(), "商品不存在或已被删除");
            }
            product.setId(existing.getId());
            int rows = productMapper.updateFields(product, now);
            if (rows == 0) {
                throw new BizException(ResultCode.FAIL.getCode(), "商品不存在或已被删除");
            }
            log.info("更新商品: id={} name={} status={}", product.getId(), product.getName(), status);
        }
        syncSkus(product.getId(), form.getSkus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_ON && status != STATUS_OFF)) {
            throw new BizException(ResultCode.FAIL.getCode(), "状态只能是 1 上架 / 0 下架");
        }
        Product existing = id == null ? null : productMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "商品不存在或已被删除");
        }
        int rows = productMapper.updateStatus(id, status, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.FAIL.getCode(), "商品不存在或已被删除");
        }
        log.info("商品{}: id={} name={}", status == STATUS_ON ? "上架" : "下架", id, existing.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Product existing = id == null ? null : productMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "商品不存在或已被删除");
        }
        long unfinished = orderItemMapper.countUnfinishedByProduct(id);
        if (unfinished > 0) {
            throw new BizException(ResultCode.PRODUCT_IN_USE.getCode(),
                    "该商品还有 " + unfinished + " 条未完结订单，无法删除");
        }
        // SPU 与它的 SKU 一起逻辑删除，避免孤儿 SKU 在别的页面冒出来
        itemMapper.logicDeleteByProduct(id, LocalDateTime.now());
        productMapper.deleteById(id);
        log.info("删除商品: id={} name={}", id, existing.getName());
    }

    // ------------------------------------------------------------------ 内部方法

    /** SKU 子表同步：新增插入、已有按 version 覆盖、表单里删掉的逻辑删除 */
    private void syncSkus(Long productId, List<AdminSkuForm> skus) {
        LocalDateTime now = LocalDateTime.now();
        List<Item> current = itemsOf(productId);
        Set<Long> keep = new HashSet<>();
        for (AdminSkuForm sku : skus) {
            Item item = new Item();
            item.setProductId(productId);
            item.setSpec(sku.getSpec().trim());
            item.setPrice(sku.getPrice().setScale(2, RoundingMode.HALF_UP));
            item.setStock(sku.getStock());
            item.setImage(blankToNull(sku.getImage()));

            if (sku.getId() == null) {
                item.setVersion(0);
                itemMapper.insert(item);
                keep.add(item.getId());
                continue;
            }
            item.setId(sku.getId());
            item.setVersion(sku.getVersion() == null ? 0 : sku.getVersion());
            int rows = itemMapper.updateSkuWithVersion(item, now);
            if (rows == 0) {
                throw new BizException(ResultCode.FAIL.getCode(),
                        "SKU「" + item.getSpec() + "」已被其他操作修改，请刷新后重试");
            }
            keep.add(item.getId());
        }
        for (Item old : current) {
            if (!keep.contains(old.getId())) {
                itemMapper.deleteById(old.getId());
            }
        }
    }

    private void validateSkus(List<AdminSkuForm> skus) {
        if (skus == null || skus.isEmpty()) {
            throw new BizException(ResultCode.FAIL.getCode(), "至少保留一个 SKU");
        }
        Set<String> specs = new HashSet<>();
        for (AdminSkuForm sku : skus) {
            String spec = sku.getSpec() == null ? "" : sku.getSpec().trim();
            if (spec.isEmpty()) {
                throw new BizException(ResultCode.FAIL.getCode(), "请输入 SKU 规格");
            }
            if (!specs.add(spec)) {
                throw new BizException(ResultCode.FAIL.getCode(), "同一商品内 SKU 规格不能重复：" + spec);
            }
            if (sku.getPrice() == null
                    || sku.getPrice().compareTo(PRICE_MIN) < 0
                    || sku.getPrice().compareTo(PRICE_MAX) > 0) {
                throw new BizException(ResultCode.FAIL.getCode(), "SKU「" + spec + "」价格需在 0.01–99999.99 之间");
            }
            if (sku.getStock() == null || sku.getStock() < 0 || sku.getStock() > STOCK_MAX) {
                throw new BizException(ResultCode.FAIL.getCode(), "SKU「" + spec + "」库存需在 0–999999 之间");
            }
        }
    }

    private Map<String, String> normalizeSpecs(List<SpecPair> pairs) {
        Map<String, String> specs = new LinkedHashMap<>();
        if (pairs == null) {
            return specs;
        }
        for (SpecPair pair : pairs) {
            if (pair == null || !StringUtils.hasText(pair.getKey())) {
                continue;
            }
            String key = pair.getKey().trim();
            String value = pair.getValue() == null ? "" : pair.getValue().trim();
            if (specs.containsKey(key)) {
                throw new BizException(ResultCode.FAIL.getCode(), "商品参数重复：" + key);
            }
            specs.put(key, value);
        }
        if (specs.size() > SPEC_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(), "商品参数最多 8 组");
        }
        return specs;
    }

    private String toJson(Map<String, String> specs) {
        try {
            return objectMapper.writeValueAsString(specs);
        } catch (Exception e) {
            throw new BizException(ResultCode.FAIL.getCode(), "商品参数格式不正确");
        }
    }

    private List<String> splitImages(String images) {
        List<String> list = new ArrayList<>();
        if (!StringUtils.hasText(images)) {
            return list;
        }
        for (String part : images.split(",")) {
            String url = part.trim();
            if (!url.isEmpty() && !list.contains(url)) {
                list.add(url);
            }
        }
        return list;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
