package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ipc.mall.common.constant.ProductStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.entity.Cart;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.CartMapper;
import com.ipc.mall.mapper.ItemMapper;
import com.ipc.mall.mapper.ProductMapper;
import com.ipc.mall.service.CartService;
import com.ipc.mall.vo.CartItemVO;
import com.ipc.mall.vo.CartVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 购物车实现（PRD §5.9 / docs/api.md F15–F18）。
 * <p>
 * 三个必须守住的规则：
 * <ol>
 *     <li><b>复活而不是新增</b>：加购先查含已删除记录，命中已删除行就把它 deleted 置 0，
 *         否则同一 (account_id, item_id) 会出现两行、合计翻倍（PRD §7.5 第 2 个坑）。</li>
 *     <li><b>勾选不落库</b>：cart 表没有 checked 列，勾选只在页面状态里。</li>
 *     <li><b>金额全用 BigDecimal</b>，小计 = 单价 × 数量，两位小数。</li>
 * </ol>
 */
@Slf4j
@Service
public class CartServiceImpl implements CartService {

    /** 单 SKU 数量上下限（PRD §5.9：1–99，数据库 CHECK 兜底） */
    static final int QTY_MIN = 1;
    static final int QTY_MAX = 99;

    private static final String INVALID_NAME = "已失效商品";
    private static final String REASON_DELETED = "商品已被删除";
    private static final String REASON_OFF_SALE = "商品已下架";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final CartMapper cartMapper;
    private final ItemMapper itemMapper;
    private final ProductMapper productMapper;

    public CartServiceImpl(CartMapper cartMapper, ItemMapper itemMapper, ProductMapper productMapper) {
        this.cartMapper = cartMapper;
        this.itemMapper = itemMapper;
        this.productMapper = productMapper;
    }

    // ------------------------------------------------------------------ 写操作

    @Override
    @Transactional
    public void add(Long accountId, Long itemId, int quantity) {
        checkQuantity(quantity);

        Item sku = itemId == null ? null : itemMapper.selectById(itemId);
        if (sku == null) {
            throw new BizException(ResultCode.PRODUCT_OFF_SALE);
        }
        Product product = sku.getProductId() == null ? null : productMapper.selectById(sku.getProductId());
        if (product == null || !ProductStatus.isOnSale(product.getStatus())) {
            throw new BizException(ResultCode.PRODUCT_OFF_SALE);
        }
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        if (quantity > stock) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }

        Cart existing = cartMapper.selectIncludingDeleted(accountId, itemId);
        if (existing == null) {
            Cart cart = new Cart();
            cart.setAccountId(accountId);
            cart.setItemId(itemId);
            cart.setQuantity(quantity);
            cartMapper.insert(cart);
            return;
        }

        // 已删除的行按「本次数量」重新开始，不把删除前的旧数量算进来
        int base = isAlive(existing) ? quantityOf(existing) : 0;
        int target = base + quantity;
        if (target > stock) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }
        // 单 SKU 上限 99：库存充足时封顶而不是拒绝
        target = Math.min(target, QTY_MAX);

        // 累加与复活走同一条条件更新：不新增行、不改 create_time
        int rows = cartMapper.updateQuantityAndRevive(existing.getId(), accountId, target, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.NO_PERMISSION);
        }
        if (isAlive(existing)) {
            log.debug("购物车累加: account={} item={} {} -> {}", accountId, itemId, existing.getQuantity(), target);
        } else {
            log.debug("购物车复活: account={} item={} 行={} 数量={}", accountId, itemId, existing.getId(), target);
        }
    }

    @Override
    @Transactional
    public void updateQuantity(Long accountId, Long cartId, int quantity) {
        checkQuantity(quantity);

        Cart cart = cartId == null ? null : cartMapper.selectById(cartId);
        if (cart == null || !Objects.equals(cart.getAccountId(), accountId)) {
            throw new BizException(ResultCode.NO_PERMISSION);
        }
        Item sku = itemMapper.selectById(cart.getItemId());
        if (sku == null) {
            // 商品已被删除：这行已经是失效数据，不再允许改数量
            throw new BizException(ResultCode.PRODUCT_OFF_SALE);
        }
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        if (quantity > stock) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }

        // 条件更新兜住并发：影响 0 行说明行已被删 / 归属变了
        int rows = cartMapper.updateQuantityOwned(cartId, accountId, quantity, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.NO_PERMISSION);
        }
    }

    @Override
    @Transactional
    public void remove(Long accountId, Long cartId) {
        if (cartId == null) {
            throw new BizException(ResultCode.NO_PERMISSION);
        }
        // 逻辑删除 + 归属校验一条 SQL 完成，影响 0 行 = 不存在或越权
        int rows = cartMapper.logicDeleteOwned(cartId, accountId, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.NO_PERMISSION);
        }
    }

    // ------------------------------------------------------------------ 读操作

    @Override
    public CartVO list(Long accountId) {
        CartVO vo = new CartVO();
        if (accountId == null) {
            return vo;
        }
        List<Cart> carts = cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getAccountId, accountId)
                .orderByDesc(Cart::getCreateTime)
                .orderByDesc(Cart::getId));
        if (carts.isEmpty()) {
            return vo;
        }

        // 两次批量查询代替 N+1：SKU 与 SPU 都可能已被逻辑删除（MP 自动过滤 deleted = 0）
        Map<Long, Item> skuMap = new HashMap<>();
        List<Long> itemIds = carts.stream().map(Cart::getItemId).filter(Objects::nonNull).distinct().toList();
        if (!itemIds.isEmpty()) {
            for (Item sku : itemMapper.selectBatchIds(itemIds)) {
                skuMap.put(sku.getId(), sku);
            }
        }
        Map<Long, Product> productMap = new HashMap<>();
        List<Long> productIds = skuMap.values().stream()
                .map(Item::getProductId).filter(Objects::nonNull).distinct().toList();
        if (!productIds.isEmpty()) {
            for (Product product : productMapper.selectBatchIds(productIds)) {
                productMap.put(product.getId(), product);
            }
        }

        List<CartItemVO> items = new ArrayList<>(carts.size());
        int totalCount = 0;
        BigDecimal totalAmount = ZERO;
        for (Cart cart : carts) {
            CartItemVO item = buildItem(cart, skuMap.get(cart.getItemId()), productMap);
            items.add(item);
            totalCount += quantityOf(cart);
            if (item.isAvailable()) {
                totalAmount = totalAmount.add(item.getSubtotal());
            }
        }
        vo.setItems(items);
        vo.setTotalCount(totalCount);
        vo.setTotalAmount(totalAmount);
        return vo;
    }

    @Override
    public int count(Long accountId) {
        if (accountId == null) {
            return 0;
        }
        return cartMapper.sumQuantityByAccount(accountId);
    }

    // ------------------------------------------------------------------ 内部方法

    private CartItemVO buildItem(Cart cart, Item sku, Map<Long, Product> productMap) {
        CartItemVO item = new CartItemVO();
        item.setCartId(cart.getId());
        item.setItemId(cart.getItemId());
        int quantity = quantityOf(cart);
        item.setQuantity(quantity);

        Product product = sku == null ? null : productMap.get(sku.getProductId());
        if (sku == null || product == null) {
            // SKU 或 SPU 已被逻辑删除：置灰、不参与合计
            item.setProductName(INVALID_NAME);
            item.setPrice(ZERO);
            item.setStock(0);
            item.setSubtotal(ZERO);
            item.setMaxQty(QTY_MAX);
            item.setAvailable(false);
            item.setUnavailableReason(REASON_DELETED);
            item.setPriceText(textOf(ZERO));
            item.setSubtotalText(textOf(ZERO));
            return item;
        }

        boolean onSale = ProductStatus.isOnSale(product.getStatus());
        BigDecimal price = sku.getPrice() == null ? ZERO : sku.getPrice().setScale(2, RoundingMode.HALF_UP);
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setSpec(sku.getSpec());
        item.setPrice(price);
        item.setPriceText(textOf(price));
        item.setStock(stock);
        item.setImage(StringUtils.hasText(sku.getImage()) ? sku.getImage() : product.getMainImage());
        item.setSubtotal(subtotal);
        item.setSubtotalText(textOf(subtotal));
        // 上限取 max(库存, 当前数量)：库存变少时不把用户已加的数量悄悄改小，交给结算二次校验
        item.setMaxQty(Math.min(QTY_MAX, Math.max(stock, quantity)));
        item.setAvailable(onSale);
        item.setUnavailableReason(onSale ? null : REASON_OFF_SALE);
        return item;
    }

    private void checkQuantity(int quantity) {
        if (quantity < QTY_MIN || quantity > QTY_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(), "商品数量需在 1–99 之间");
        }
    }

    private int quantityOf(Cart cart) {
        return cart.getQuantity() == null ? 0 : cart.getQuantity();
    }

    private boolean isAlive(Cart cart) {
        return cart.getDeleted() == null || cart.getDeleted() == 0;
    }

    private String textOf(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
