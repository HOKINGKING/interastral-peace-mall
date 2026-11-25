package com.ipc.mall.admin;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.AdminSkuForm;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.ItemMapper;
import com.ipc.mall.mapper.ProductMapper;
import com.ipc.mall.service.impl.ItemAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 后台 SKU 管理（PRD §6.3 A14–A16）：规格唯一、价格/库存范围、版本乐观锁、最后一条禁删。
 */
class ItemAdminServiceImplTest {

    private ItemMapper itemMapper;
    private ProductMapper productMapper;
    private ItemAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        itemMapper = mock(ItemMapper.class);
        productMapper = mock(ProductMapper.class);
        service = new ItemAdminServiceImpl(itemMapper, productMapper);
        when(productMapper.selectById(5L)).thenReturn(product(5L));
    }

    @Test
    void 列表按SPU查询() {
        when(itemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(item(10L, "标准", 3)));

        List<Item> items = service.listByProduct(5L);

        assertEquals(1, items.size());
        assertEquals("标准", items.get(0).getSpec());
    }

    @Test
    void 缺少所属商品时拒绝保存() {
        AdminSkuForm form = form(null, "标准", "59.00", 10, null);
        form.setProductId(null);

        BizException exception = assertThrows(BizException.class, () -> service.save(form));

        assertTrue(exception.getMessage().contains("所属商品"));
    }

    @Test
    void 商品不存在时拒绝保存() {
        when(productMapper.selectById(9L)).thenReturn(null);
        AdminSkuForm form = form(null, "标准", "59.00", 10, null);
        form.setProductId(9L);

        BizException exception = assertThrows(BizException.class, () -> service.save(form));

        assertTrue(exception.getMessage().contains("商品不存在"));
    }

    @Test
    void 同一商品内规格重复被拒绝() {
        when(itemMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BizException exception = assertThrows(BizException.class, () -> service.save(form(null, "标准", "59.00", 10, null)));

        assertTrue(exception.getMessage().contains("规格"));
        verify(itemMapper, never()).insert(any(Item.class));
    }

    @Test
    void 价格与库存越界被拒绝() {
        AdminSkuForm cheap = form(null, "标准", "0.00", 10, null);
        assertTrue(assertThrows(BizException.class, () -> service.save(cheap)).getMessage().contains("价格"));

        AdminSkuForm hugeStock = form(null, "标准", "59.00", 1000000, null);
        assertTrue(assertThrows(BizException.class, () -> service.save(hugeStock)).getMessage().contains("库存"));
    }

    @Test
    void 新增SKU版本号从零开始() {
        when(itemMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(itemMapper.insert(any(Item.class))).thenAnswer(invocation -> {
            ((Item) invocation.getArgument(0)).setId(7L);
            return 1;
        });

        service.save(form(null, "  500ml 星白 ", "59.00", 10, null));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).insert(captor.capture());
        Item saved = captor.getValue();
        assertEquals(5L, saved.getProductId());
        assertEquals("500ml 星白", saved.getSpec());
        assertEquals("59.00", saved.getPrice().toPlainString());
        assertEquals(0, saved.getVersion());
    }

    @Test
    void 编辑缺少版本号时拒绝() {
        when(itemMapper.selectById(10L)).thenReturn(item(10L, "标准", 3));

        BizException exception = assertThrows(BizException.class, () -> service.save(form(10L, "标准", "59.00", 10, null)));

        assertTrue(exception.getMessage().contains("版本号"));
    }

    @Test
    void 编辑版本冲突提示刷新() {
        when(itemMapper.selectById(10L)).thenReturn(item(10L, "标准", 3));
        when(itemMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(itemMapper.updateSkuWithVersion(any(Item.class), any(LocalDateTime.class))).thenReturn(0);

        BizException exception = assertThrows(BizException.class, () -> service.save(form(10L, "标准", "59.00", 10, 2)));

        assertTrue(exception.getMessage().contains("刷新"));
    }

    @Test
    void 编辑成功走带版本的整行更新() {
        when(itemMapper.selectById(10L)).thenReturn(item(10L, "标准", 3));
        when(itemMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(itemMapper.updateSkuWithVersion(any(Item.class), any(LocalDateTime.class))).thenReturn(1);

        service.save(form(10L, "标准升级", "69.00", 5, 3));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).updateSkuWithVersion(captor.capture(), any(LocalDateTime.class));
        assertEquals(10L, captor.getValue().getId());
        assertEquals("标准升级", captor.getValue().getSpec());
        assertEquals(3, captor.getValue().getVersion());
    }

    @Test
    void 最后一条SKU不能删() {
        when(itemMapper.selectById(10L)).thenReturn(item(10L, "标准", 0));
        when(itemMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BizException exception = assertThrows(BizException.class, () -> service.remove(10L));

        assertTrue(exception.getMessage().contains("至少保留一个"));
        verify(itemMapper, never()).deleteById(anyLong());
    }

    @Test
    void 有多条SKU时可以删() {
        when(itemMapper.selectById(10L)).thenReturn(item(10L, "标准", 0));
        when(itemMapper.selectCount(any(Wrapper.class))).thenReturn(2L);

        service.remove(10L);

        verify(itemMapper).deleteById(10L);
    }

    @Test
    void 删除不存在的SKU返回四百() {
        when(itemMapper.selectById(10L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> service.remove(10L));
        assertEquals(ResultCode.FAIL.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 夹具

    private AdminSkuForm form(Long id, String spec, String price, int stock, Integer version) {
        AdminSkuForm form = new AdminSkuForm();
        form.setId(id);
        form.setProductId(5L);
        form.setSpec(spec);
        form.setPrice(new BigDecimal(price));
        form.setStock(stock);
        form.setVersion(version);
        return form;
    }

    private Item item(Long id, String spec, int version) {
        Item item = new Item();
        item.setId(id);
        item.setProductId(5L);
        item.setSpec(spec);
        item.setPrice(new BigDecimal("59.00"));
        item.setStock(10);
        item.setVersion(version);
        return item;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("星际和平水杯");
        product.setCategoryId(101L);
        product.setStatus(1);
        return product;
    }
}
