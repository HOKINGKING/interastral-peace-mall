package com.ipc.mall.cart;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ipc.mall.common.constant.ProductStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.entity.Cart;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.CartMapper;
import com.ipc.mall.mapper.ItemMapper;
import com.ipc.mall.mapper.ProductMapper;
import com.ipc.mall.service.impl.CartServiceImpl;
import com.ipc.mall.vo.CartItemVO;
import com.ipc.mall.vo.CartVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 购物车业务规则（PRD §5.9 / handoff §8）。
 * <p>
 * 重点覆盖三个坑：复活不新增、累加封顶、失效商品置灰不参与合计。
 */
class CartServiceImplTest {

    private CartMapper cartMapper;
    private ItemMapper itemMapper;
    private ProductMapper productMapper;
    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        cartMapper = mock(CartMapper.class);
        itemMapper = mock(ItemMapper.class);
        productMapper = mock(ProductMapper.class);
        cartService = new CartServiceImpl(cartMapper, itemMapper, productMapper);
    }

    // ------------------------------------------------------------ 加购

    @Test
    void 新SKU加购插入一行() {
        when(itemMapper.selectById(6L)).thenReturn(sku(6L, 4L, "39.00", 240));
        when(productMapper.selectById(4L)).thenReturn(product(4L, ProductStatus.ON_SALE));
        when(cartMapper.selectIncludingDeleted(1L, 6L)).thenReturn(null);

        cartService.add(1L, 6L, 2);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartMapper).insert(captor.capture());
        Cart saved = captor.getValue();
        assertEquals(1L, saved.getAccountId());
        assertEquals(6L, saved.getItemId());
        assertEquals(2, saved.getQuantity());
        verify(cartMapper, never()).updateQuantityAndRevive(any(), any(), any(), any());
    }

    @Test
    void 同SKU重复加购累加数量不新增() {
        when(itemMapper.selectById(6L)).thenReturn(sku(6L, 4L, "39.00", 240));
        when(productMapper.selectById(4L)).thenReturn(product(4L, ProductStatus.ON_SALE));
        when(cartMapper.selectIncludingDeleted(1L, 6L)).thenReturn(cart(9L, 1L, 6L, 2, 0));
        when(cartMapper.updateQuantityAndRevive(eq(9L), eq(1L), anyInt(), any(LocalDateTime.class)))
                .thenReturn(1);

        cartService.add(1L, 6L, 1);

        // 2 + 1 = 3，走条件更新而不是 insert
        verify(cartMapper).updateQuantityAndRevive(eq(9L), eq(1L), eq(3), any(LocalDateTime.class));
        verify(cartMapper, never()).insert(any(Cart.class));
    }

    @Test
    void 已逻辑删除的同SKU加购走复活且数量从本次算起() {
        when(itemMapper.selectById(6L)).thenReturn(sku(6L, 4L, "39.00", 240));
        when(productMapper.selectById(4L)).thenReturn(product(4L, ProductStatus.ON_SALE));
        // deleted = 1 且历史数量 5：复活后应是 2（本次数量），不是 7
        when(cartMapper.selectIncludingDeleted(1L, 6L)).thenReturn(cart(9L, 1L, 6L, 5, 1));
        when(cartMapper.updateQuantityAndRevive(eq(9L), eq(1L), anyInt(), any(LocalDateTime.class)))
                .thenReturn(1);

        cartService.add(1L, 6L, 2);

        verify(cartMapper, never()).insert(any(Cart.class));
        verify(cartMapper).updateQuantityAndRevive(eq(9L), eq(1L), eq(2), any(LocalDateTime.class));
    }

    @Test
    void 累加超过99时封顶到99() {
        when(itemMapper.selectById(6L)).thenReturn(sku(6L, 4L, "39.00", 240));
        when(productMapper.selectById(4L)).thenReturn(product(4L, ProductStatus.ON_SALE));
        when(cartMapper.selectIncludingDeleted(1L, 6L)).thenReturn(cart(9L, 1L, 6L, 98, 0));
        when(cartMapper.updateQuantityAndRevive(eq(9L), eq(1L), anyInt(), any(LocalDateTime.class)))
                .thenReturn(1);

        cartService.add(1L, 6L, 5);

        verify(cartMapper).updateQuantityAndRevive(eq(9L), eq(1L), eq(99), any(LocalDateTime.class));
    }

    @Test
    void 累计数量超过库存返回两千零二() {
        when(itemMapper.selectById(6L)).thenReturn(sku(6L, 4L, "39.00", 3));
        when(productMapper.selectById(4L)).thenReturn(product(4L, ProductStatus.ON_SALE));
        when(cartMapper.selectIncludingDeleted(1L, 6L)).thenReturn(cart(9L, 1L, 6L, 2, 0));

        BizException exception = assertThrows(BizException.class, () -> cartService.add(1L, 6L, 2));
        assertEquals(ResultCode.STOCK_NOT_ENOUGH.getCode(), exception.getCode());
        verify(cartMapper, never()).updateQuantityAndRevive(any(), any(), any(), any());
    }

    @Test
    void 单次加购超过库存返回两千零二() {
        when(itemMapper.selectById(6L)).thenReturn(sku(6L, 4L, "39.00", 3));
        when(productMapper.selectById(4L)).thenReturn(product(4L, ProductStatus.ON_SALE));

        BizException exception = assertThrows(BizException.class, () -> cartService.add(1L, 6L, 5));
        assertEquals(ResultCode.STOCK_NOT_ENOUGH.getCode(), exception.getCode());
    }

    @Test
    void 商品已下架返回两千零一() {
        when(itemMapper.selectById(6L)).thenReturn(sku(6L, 4L, "39.00", 240));
        when(productMapper.selectById(4L)).thenReturn(product(4L, ProductStatus.OFF_SALE));

        BizException exception = assertThrows(BizException.class, () -> cartService.add(1L, 6L, 1));
        assertEquals(ResultCode.PRODUCT_OFF_SALE.getCode(), exception.getCode());
    }

    @Test
    void SKU已被删除返回两千零一() {
        when(itemMapper.selectById(6L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> cartService.add(1L, 6L, 1));
        assertEquals(ResultCode.PRODUCT_OFF_SALE.getCode(), exception.getCode());
    }

    @Test
    void 数量非法返回四百() {
        BizException zero = assertThrows(BizException.class, () -> cartService.add(1L, 6L, 0));
        assertEquals(ResultCode.FAIL.getCode(), zero.getCode());

        BizException tooMany = assertThrows(BizException.class, () -> cartService.add(1L, 6L, 100));
        assertEquals(ResultCode.FAIL.getCode(), tooMany.getCode());
    }

    // ------------------------------------------------------------ 改数量

    @Test
    void 修改数量成功走条件更新() {
        when(cartMapper.selectById(2L)).thenReturn(cart(2L, 1L, 8L, 1, 0));
        when(itemMapper.selectById(8L)).thenReturn(sku(8L, 6L, "49.00", 88));
        when(cartMapper.updateQuantityOwned(eq(2L), eq(1L), eq(5), any(LocalDateTime.class))).thenReturn(1);

        cartService.updateQuantity(1L, 2L, 5);

        verify(cartMapper).updateQuantityOwned(eq(2L), eq(1L), eq(5), any(LocalDateTime.class));
    }

    @Test
    void 修改数量超过库存返回两千零二() {
        when(cartMapper.selectById(2L)).thenReturn(cart(2L, 1L, 5L, 1, 0));
        when(itemMapper.selectById(5L)).thenReturn(sku(5L, 3L, "299.00", 12));

        BizException exception = assertThrows(BizException.class,
                () -> cartService.updateQuantity(1L, 2L, 99));
        assertEquals(ResultCode.STOCK_NOT_ENOUGH.getCode(), exception.getCode());
        verify(cartMapper, never()).updateQuantityOwned(any(), any(), any(), any());
    }

    @Test
    void 改别人的购物车行返回三千零四() {
        when(cartMapper.selectById(2L)).thenReturn(cart(2L, 1L, 8L, 1, 0));

        // bob(2L) 改 xingye(1L) 的行
        BizException exception = assertThrows(BizException.class,
                () -> cartService.updateQuantity(2L, 2L, 3));
        assertEquals(ResultCode.NO_PERMISSION.getCode(), exception.getCode());
        verify(cartMapper, never()).updateQuantityOwned(any(), any(), any(), any());
    }

    @Test
    void 条件更新零行视为越权() {
        when(cartMapper.selectById(2L)).thenReturn(cart(2L, 1L, 8L, 1, 0));
        when(itemMapper.selectById(8L)).thenReturn(sku(8L, 6L, "49.00", 88));
        when(cartMapper.updateQuantityOwned(eq(2L), eq(1L), eq(3), any(LocalDateTime.class))).thenReturn(0);

        BizException exception = assertThrows(BizException.class,
                () -> cartService.updateQuantity(1L, 2L, 3));
        assertEquals(ResultCode.NO_PERMISSION.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 删除

    @Test
    void 删除走逻辑删除并校验归属() {
        when(cartMapper.logicDeleteOwned(eq(2L), eq(1L), any(LocalDateTime.class))).thenReturn(1);

        cartService.remove(1L, 2L);

        verify(cartMapper).logicDeleteOwned(eq(2L), eq(1L), any(LocalDateTime.class));
        verify(cartMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void 删除别人的购物车行返回三千零四() {
        when(cartMapper.logicDeleteOwned(eq(2L), eq(2L), any(LocalDateTime.class))).thenReturn(0);

        BizException exception = assertThrows(BizException.class, () -> cartService.remove(2L, 2L));
        assertEquals(ResultCode.NO_PERMISSION.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 列表 / 角标

    @Test
    void 列表标记失效商品且不计入可结算金额() {
        when(cartMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                cart(3L, 1L, 4L, 2, 0),   // 徽章 39.00 × 2 = 78.00
                cart(2L, 1L, 9L, 1, 0),   // 商品已下架
                cart(1L, 1L, 99L, 1, 0))); // SKU 已被逻辑删除
        when(itemMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                sku(4L, 4L, "39.00", 240),
                sku(9L, 9L, "69.00", 54)));
        when(productMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                product(4L, ProductStatus.ON_SALE),
                product(9L, ProductStatus.OFF_SALE)));

        CartVO cart = cartService.list(1L);

        assertEquals(3, cart.getItems().size());
        CartItemVO normal = cart.getItems().get(0);
        assertTrue(normal.isAvailable());
        assertNull(normal.getUnavailableReason());
        assertEquals("78.00", normal.getSubtotalText());
        assertEquals("徽章", normal.getProductName());

        CartItemVO offSale = cart.getItems().get(1);
        assertFalse(offSale.isAvailable());
        assertEquals("商品已下架", offSale.getUnavailableReason());

        CartItemVO deleted = cart.getItems().get(2);
        assertFalse(deleted.isAvailable());
        assertEquals("商品已被删除", deleted.getUnavailableReason());
        assertEquals("0.00", deleted.getSubtotalText());

        // 件数含失效行，金额只算可结算行
        assertEquals(4, cart.getTotalCount());
        assertEquals(0, new BigDecimal("78.00").compareTo(cart.getTotalAmount()));
    }

    @Test
    void 空购物车返回空列表() {
        when(cartMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CartVO cart = cartService.list(1L);

        assertTrue(cart.getItems().isEmpty());
        assertEquals(0, cart.getTotalCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(cart.getTotalAmount()));
    }

    @Test
    void 角标取商品总件数() {
        when(cartMapper.sumQuantityByAccount(1L)).thenReturn(3);

        assertEquals(3, cartService.count(1L));
        assertEquals(0, cartService.count(null));
        verify(cartMapper, never()).sumQuantityByAccount(null);
    }

    // ------------------------------------------------------------ 测试夹具

    private Cart cart(Long id, Long accountId, Long itemId, int quantity, int deleted) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setAccountId(accountId);
        cart.setItemId(itemId);
        cart.setQuantity(quantity);
        cart.setDeleted(deleted);
        return cart;
    }

    private Item sku(Long id, Long productId, String price, int stock) {
        Item item = new Item();
        item.setId(id);
        item.setProductId(productId);
        item.setSpec("测试规格");
        item.setPrice(new BigDecimal(price));
        item.setStock(stock);
        item.setVersion(0);
        return item;
    }

    private Product product(Long id, int status) {
        Product product = new Product();
        product.setId(id);
        product.setName(id == 4L ? "徽章" : "测试商品");
        product.setStatus(status);
        product.setMainImage(null);
        return product;
    }
}
