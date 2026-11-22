package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipc.mall.common.constant.ProductStatus;
import com.ipc.mall.dto.ProductQuery;
import com.ipc.mall.entity.Category;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.CategoryMapper;
import com.ipc.mall.mapper.ItemMapper;
import com.ipc.mall.mapper.ProductMapper;
import com.ipc.mall.service.ProductService;
import com.ipc.mall.vo.ProductCardVO;
import com.ipc.mall.vo.ProductDetailVO;
import com.ipc.mall.vo.SkuVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 前台商品读取实现（PRD §5.1–§5.4）。
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    /** 相册最多 5 张（PRD §5.4） */
    private static final int IMAGE_MAX = 5;
    /** 单次购买数量上限（PRD §5.4：min(stock, 99)） */
    private static final int QTY_MAX = 99;
    /** 看了又看取 4 条 */
    private static final int RELATED_LIMIT = 4;

    private final ProductMapper productMapper;
    private final ItemMapper itemMapper;
    private final CategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    public ProductServiceImpl(ProductMapper productMapper,
                              ItemMapper itemMapper,
                              CategoryMapper categoryMapper,
                              ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.itemMapper = itemMapper;
        this.categoryMapper = categoryMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ProductCardVO> recommended(int limit) {
        int size = Math.max(1, limit);
        ProductQuery query = new ProductQuery();
        query.setSize(size);
        query.normalize();
        Page<ProductCardVO> page = new Page<>(1, size);
        // 首页推荐不需要总数，省一次 count 查询
        page.setSearchCount(false);
        return productMapper.selectCardPage(page, query, null).getRecords();
    }

    @Override
    public IPage<ProductCardVO> pageCards(ProductQuery query, List<Long> categoryIds) {
        Page<ProductCardVO> page = new Page<>(query.getPage(), query.getSize());
        return productMapper.selectCardPage(page, query, categoryIds);
    }

    @Override
    public Optional<ProductDetailVO> detail(Long productId) {
        if (productId == null) {
            return Optional.empty();
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Optional.empty();
        }

        List<SkuVO> skus = skusOf(productId);
        ProductDetailVO detail = new ProductDetailVO();
        detail.setProduct(product);
        detail.setSkus(skus);
        detail.setImages(imagesOf(product));
        detail.setSpecs(parseSpecs(product.getSpecs()));

        Category category = product.getCategoryId() == null
                ? null
                : categoryMapper.selectById(product.getCategoryId());
        detail.setCategory(category);
        if (category != null && category.getParentId() != null && category.getParentId() != 0L) {
            detail.setParentCategory(categoryMapper.selectById(category.getParentId()));
        }

        detail.setMinPrice(minPriceOf(skus));
        int totalStock = totalStockOf(skus);
        detail.setTotalStock(totalStock);
        detail.setSoldOut(totalStock <= 0);
        detail.setOffSale(!ProductStatus.isOnSale(product.getStatus()));

        SkuVO defaultSku = skus.stream()
                .filter(sku -> !sku.isSoldOut())
                .findFirst()
                .orElse(skus.isEmpty() ? null : skus.get(0));
        detail.setDefaultSku(defaultSku);
        detail.setMaxQty(defaultSku == null || defaultSku.isSoldOut()
                ? 0
                : Math.min(defaultSku.getStock(), QTY_MAX));

        detail.setRelated(relatedOf(product, category));
        return Optional.of(detail);
    }

    @Override
    public List<SkuVO> skusOf(Long productId) {
        if (productId == null) {
            return List.of();
        }
        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getProductId, productId)
                .orderByAsc(Item::getId));
        List<SkuVO> skus = new ArrayList<>(items.size());
        for (Item item : items) {
            SkuVO sku = new SkuVO();
            sku.setId(item.getId());
            sku.setSpec(item.getSpec());
            sku.setPrice(item.getPrice());
            sku.setPriceText(item.getPrice() == null
                    ? "0.00"
                    : item.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString());
            sku.setStock(item.getStock());
            sku.setImage(item.getImage());
            skus.add(sku);
        }
        return skus;
    }

    @Override
    public boolean exists(Long productId) {
        return productId != null && productMapper.selectById(productId) != null;
    }

    // ------------------------------------------------------------------ 内部方法

    private List<ProductCardVO> relatedOf(Product product, Category category) {
        if (category == null) {
            return List.of();
        }
        ProductQuery query = new ProductQuery();
        query.setSize(RELATED_LIMIT + 1);
        query.normalize();
        Page<ProductCardVO> page = new Page<>(1, RELATED_LIMIT + 1);
        page.setSearchCount(false);
        List<ProductCardVO> candidates = productMapper
                .selectCardPage(page, query, List.of(category.getId()))
                .getRecords();
        return candidates.stream()
                .filter(card -> !Objects.equals(card.getId(), product.getId()))
                .limit(RELATED_LIMIT)
                .toList();
    }

    private List<String> imagesOf(Product product) {
        List<String> images = new ArrayList<>();
        if (StringUtils.hasText(product.getImages())) {
            for (String part : product.getImages().split(",")) {
                String url = part.trim();
                if (!url.isEmpty() && !images.contains(url)) {
                    images.add(url);
                }
                if (images.size() >= IMAGE_MAX) {
                    break;
                }
            }
        }
        if (StringUtils.hasText(product.getMainImage())) {
            String main = product.getMainImage().trim();
            if (!images.contains(main)) {
                images.add(0, main);
            }
        }
        return images.size() > IMAGE_MAX ? new ArrayList<>(images.subList(0, IMAGE_MAX)) : images;
    }

    /**
     * 商品参数存 JSON 对象；解析失败只隐藏参数区，不影响详情页其它内容（PRD §5.4）。
     */
    private Map<String, String> parseSpecs(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            Map<String, String> specs = new LinkedHashMap<>();
            raw.forEach((key, value) -> {
                if (key != null && value != null) {
                    specs.put(key, String.valueOf(value));
                }
            });
            return specs;
        } catch (Exception e) {
            log.warn("商品参数 JSON 解析失败，已忽略该字段: {}", json);
            return Collections.emptyMap();
        }
    }

    private BigDecimal minPriceOf(List<SkuVO> skus) {
        return skus.stream()
                .map(SkuVO::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private int totalStockOf(List<SkuVO> skus) {
        int total = 0;
        for (SkuVO sku : skus) {
            if (sku.getStock() != null && sku.getStock() > 0) {
                total += sku.getStock();
            }
        }
        return total;
    }
}
