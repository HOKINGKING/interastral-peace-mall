package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.AdminSkuForm;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.ItemMapper;
import com.ipc.mall.mapper.ProductMapper;
import com.ipc.mall.service.ItemAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 后台 SKU 管理实现（PRD §6.3 A14–A16）。
 * <p>
 * 编辑必须带 {@code version}（乐观锁），删除不能把 SPU 的最后一条 SKU 删掉。
 */
@Slf4j
@Service
public class ItemAdminServiceImpl implements ItemAdminService {

    private static final BigDecimal PRICE_MIN = new BigDecimal("0.01");
    private static final BigDecimal PRICE_MAX = new BigDecimal("99999.99");
    private static final int STOCK_MAX = 999999;

    private final ItemMapper itemMapper;
    private final ProductMapper productMapper;

    public ItemAdminServiceImpl(ItemMapper itemMapper, ProductMapper productMapper) {
        this.itemMapper = itemMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<Item> listByProduct(Long productId) {
        if (productId == null) {
            return List.of();
        }
        return itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getProductId, productId)
                .orderByAsc(Item::getId));
    }

    @Override
    public Optional<Item> get(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(itemMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(AdminSkuForm form) {
        if (form.getProductId() == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "缺少所属商品");
        }
        Product product = productMapper.selectById(form.getProductId());
        if (product == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "商品不存在或已被删除");
        }
        String spec = form.getSpec() == null ? "" : form.getSpec().trim();
        if (spec.isEmpty()) {
            throw new BizException(ResultCode.FAIL.getCode(), "请输入 SKU 规格");
        }
        if (form.getPrice() == null
                || form.getPrice().compareTo(PRICE_MIN) < 0
                || form.getPrice().compareTo(PRICE_MAX) > 0) {
            throw new BizException(ResultCode.FAIL.getCode(), "价格需在 0.01–99999.99 之间");
        }
        if (form.getStock() == null || form.getStock() < 0 || form.getStock() > STOCK_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(), "库存需在 0–999999 之间");
        }

        // 同 SPU 内规格唯一（排除自己），MP 自动过滤 deleted = 0
        Long duplicated = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .eq(Item::getProductId, form.getProductId())
                .eq(Item::getSpec, spec)
                .ne(form.getId() != null, Item::getId, form.getId()));
        if (duplicated != null && duplicated > 0) {
            throw new BizException(ResultCode.FAIL.getCode(), "同一商品内已存在规格：" + spec);
        }

        Item item = new Item();
        item.setProductId(form.getProductId());
        item.setSpec(spec);
        item.setPrice(form.getPrice().setScale(2, RoundingMode.HALF_UP));
        item.setStock(form.getStock());
        item.setImage(blankToNull(form.getImage()));

        if (form.getId() == null) {
            item.setVersion(0);
            itemMapper.insert(item);
            log.info("新增 SKU: productId={} spec={}", form.getProductId(), spec);
            return;
        }

        Item existing = itemMapper.selectById(form.getId());
        if (existing == null || !Objects.equals(existing.getProductId(), form.getProductId())) {
            throw new BizException(ResultCode.FAIL.getCode(), "SKU 不存在或不属于该商品");
        }
        if (form.getVersion() == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "缺少版本号，请刷新后重试");
        }
        item.setId(form.getId());
        item.setVersion(form.getVersion());
        int rows = itemMapper.updateSkuWithVersion(item, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.FAIL.getCode(), "SKU 已被其他操作修改，请刷新后重试");
        }
        log.info("更新 SKU: id={} spec={} stock={}", form.getId(), spec, form.getStock());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Item item = id == null ? null : itemMapper.selectById(id);
        if (item == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "SKU 不存在或已被删除");
        }
        Long count = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .eq(Item::getProductId, item.getProductId()));
        if (count == null || count <= 1) {
            throw new BizException(ResultCode.FAIL.getCode(), "每个商品至少保留一个 SKU，不能删除最后一条");
        }
        itemMapper.deleteById(id);
        log.info("删除 SKU: id={} spec={}", id, item.getSpec());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
