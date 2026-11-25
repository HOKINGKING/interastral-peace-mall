package com.ipc.mall.admin;

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
import com.ipc.mall.service.impl.ProductAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * 后台商品管理（PRD §6.3 A9–A13）。
 * <p>
 * 覆盖：分类必须二级、相册/参数上限、SKU 校验与乐观锁、删除前的未完结订单校验。
 */
class ProductAdminServiceImplTest {

    private ProductMapper productMapper;
    private ItemMapper itemMapper;
    private CategoryMapper categoryMapper;
    private OrderItemMapper orderItemMapper;
    private ProductAdminServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        productMapper = mock(ProductMapper.class);
        itemMapper = mock(ItemMapper.class);
        categoryMapper = mock(CategoryMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        service = new ProductAdminServiceImpl(productMapper, itemMapper, categoryMapper, orderItemMapper, objectMapper);
        when(categoryMapper.selectById(101L)).thenReturn(level2Category(101L, 1L));
        when(itemMapper.selectList(any())).thenReturn(List.of());
    }

    // ------------------------------------------------------------ 列表 / 回填

    @Test
    void 列表把关键词去空白后交给查询() {
        service.page("  水杯  ", List.of(101L), 1, 1, 10);

        verify(productMapper).selectAdminPage(any(), eq("水杯"), eq(List.of(101L)), eq(1));
    }

    @Test
    void 参数JSON解析成有序key_value() {
        Product product = product(5L);
        product.setSpecs("{\"材质\":\"316 不锈钢\",\"容量\":\"500ml\"}");

        List<SpecPair> pairs = service.specPairsOf(product);

        assertEquals(2, pairs.size());
        assertEquals("材质", pairs.get(0).getKey());
        assertEquals("500ml", pairs.get(1).getValue());
    }

    @Test
    void 参数JSON损坏时返回空列表不影响打开表单() {
        Product product = product(5L);
        product.setSpecs("not-a-json");

        assertTrue(service.specPairsOf(product).isEmpty());
    }

    @Test
    void 相册按逗号拆分去重() {
        Product product = product(5L);
        product.setImages("/uploads/a.png, /uploads/b.png ,/uploads/a.png");

        List<String> gallery = service.galleryOf(product);

        assertEquals(List.of("/uploads/a.png", "/uploads/b.png"), gallery);
    }

    // ------------------------------------------------------------ 保存校验

    @Test
    void 分类必须挂二级分类() {
        when(categoryMapper.selectById(1L)).thenReturn(rootCategory(1L));

        BizException exception = assertThrows(BizException.class, () -> service.save(form(1L)));

        assertTrue(exception.getMessage().contains("二级分类"));
        verify(productMapper, never()).insert(any(Product.class));
    }

    @Test
    void 相册超过五张被拒绝() {
        AdminProductForm form = form(101L);
        form.setImages("/uploads/1.png,/uploads/2.png,/uploads/3.png,/uploads/4.png,/uploads/5.png,/uploads/6.png");

        BizException exception = assertThrows(BizException.class, () -> service.save(form));

        assertTrue(exception.getMessage().contains("5 张"));
    }

    @Test
    void 参数超过八组被拒绝() {
        AdminProductForm form = form(101L);
        List<SpecPair> specs = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            specs.add(new SpecPair("k" + i, "v" + i));
        }
        form.setSpecs(specs);

        BizException exception = assertThrows(BizException.class, () -> service.save(form));

        assertTrue(exception.getMessage().contains("8 组"));
    }

    @Test
    void 至少保留一个SKU() {
        AdminProductForm form = form(101L);
        form.setSkus(List.of());

        BizException exception = assertThrows(BizException.class, () -> service.save(form));

        assertTrue(exception.getMessage().contains("SKU"));
    }

    @Test
    void 同一商品内SKU规格不能重复() {
        AdminProductForm form = form(101L);
        form.setSkus(List.of(sku(null, "500ml 星白", "59.00", 10, null),
                sku(null, "500ml 星白", "59.00", 10, null)));

        BizException exception = assertThrows(BizException.class, () -> service.save(form));

        assertTrue(exception.getMessage().contains("规格不能重复"));
    }

    @Test
    void SKU价格越界被拒绝() {
        AdminProductForm form = form(101L);
        form.setSkus(List.of(sku(null, "标准", "0.00", 10, null)));

        BizException exception = assertThrows(BizException.class, () -> service.save(form));

        assertTrue(exception.getMessage().contains("价格"));
    }

    @Test
    void SKU库存越界被拒绝() {
        AdminProductForm form = form(101L);
        form.setSkus(List.of(sku(null, "标准", "59.00", 1000000, null)));

        BizException exception = assertThrows(BizException.class, () -> service.save(form));

        assertTrue(exception.getMessage().contains("库存"));
    }

    // ------------------------------------------------------------ 保存成功

    @Test
    void 新建商品连同SKU一起落库() {
        AdminProductForm form = form(101L);
        form.setName("  新品水杯  ");
        form.setSpecs(List.of(new SpecPair("材质", "316 不锈钢")));
        form.setImages("/uploads/a.png,/uploads/b.png");
        form.setSkus(List.of(sku(null, "500ml 星白", "59.00", 10, null)));
        when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(99L);
            return 1;
        });
        when(itemMapper.insert(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setId(7L);
            return 1;
        });

        service.save(form);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productMapper).insert(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertEquals("新品水杯", saved.getName());
        assertEquals(101L, saved.getCategoryId());
        assertEquals(1, saved.getStatus());
        assertEquals("/uploads/a.png,/uploads/b.png", saved.getImages());
        assertTrue(saved.getSpecs().contains("316 不锈钢"));

        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).insert(itemCaptor.capture());
        Item sku = itemCaptor.getValue();
        assertEquals(99L, sku.getProductId());
        assertEquals("500ml 星白", sku.getSpec());
        assertEquals("59.00", sku.getPrice().toPlainString());
        assertEquals(10, sku.getStock());
        assertEquals(0, sku.getVersion(), "新建 SKU 版本号必须是 0");
    }

    @Test
    void 编辑商品更新SPU与已有SKU并删除被移除的行() {
        Product existing = product(5L);
        when(productMapper.selectById(5L)).thenReturn(existing);
        when(productMapper.updateFields(any(Product.class), any(LocalDateTime.class))).thenReturn(1);
        when(itemMapper.selectList(any())).thenReturn(List.of(item(10L, 5L, "旧规格", 3), item(11L, 5L, "要删的", 1)));
        when(itemMapper.updateSkuWithVersion(any(Item.class), any(LocalDateTime.class))).thenReturn(1);

        AdminProductForm form = form(101L);
        form.setId(5L);
        form.setStatus(0);
        form.setSkus(List.of(sku(10L, "新规格", "88.00", 20, 3)));

        service.save(form);

        verify(productMapper).updateFields(any(Product.class), any(LocalDateTime.class));
        verify(itemMapper).updateSkuWithVersion(any(Item.class), any(LocalDateTime.class));
        verify(itemMapper, never()).insert(any(Item.class));
        // 11 号不在表单里 → 逻辑删除
        verify(itemMapper).deleteById(11L);
        verify(itemMapper, never()).deleteById(10L);
    }

    @Test
    void SKU版本冲突提示刷新重试() {
        when(productMapper.selectById(5L)).thenReturn(product(5L));
        when(productMapper.updateFields(any(Product.class), any(LocalDateTime.class))).thenReturn(1);
        when(itemMapper.selectList(any())).thenReturn(List.of(item(10L, 5L, "规格", 3)));
        when(itemMapper.updateSkuWithVersion(any(Item.class), any(LocalDateTime.class))).thenReturn(0);

        AdminProductForm form = form(101L);
        form.setId(5L);
        form.setSkus(List.of(sku(10L, "规格", "59.00", 5, 2)));

        BizException exception = assertThrows(BizException.class, () -> service.save(form));

        assertTrue(exception.getMessage().contains("刷新"));
    }

    @Test
    void 编辑不存在的商品返回四百() {
        when(productMapper.selectById(5L)).thenReturn(null);

        AdminProductForm form = form(101L);
        form.setId(5L);

        BizException exception = assertThrows(BizException.class, () -> service.save(form));
        assertEquals(ResultCode.FAIL.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 上下架 / 删除

    @Test
    void 上下架只接受零和一() {
        BizException exception = assertThrows(BizException.class, () -> service.updateStatus(5L, 2));
        assertTrue(exception.getMessage().contains("1 上架"));
    }

    @Test
    void 上下架成功() {
        when(productMapper.selectById(5L)).thenReturn(product(5L));
        when(productMapper.updateStatus(eq(5L), eq(0), any(LocalDateTime.class))).thenReturn(1);

        service.updateStatus(5L, 0);

        verify(productMapper).updateStatus(eq(5L), eq(0), any(LocalDateTime.class));
    }

    @Test
    void 有未完结订单时不能删除() {
        when(productMapper.selectById(5L)).thenReturn(product(5L));
        when(orderItemMapper.countUnfinishedByProduct(5L)).thenReturn(2L);

        BizException exception = assertThrows(BizException.class, () -> service.remove(5L));

        assertEquals(ResultCode.PRODUCT_IN_USE.getCode(), exception.getCode());
        verify(productMapper, never()).deleteById(anyLong());
        verify(itemMapper, never()).logicDeleteByProduct(anyLong(), any());
    }

    @Test
    void 删除商品会连带逻辑删除SKU() {
        when(productMapper.selectById(5L)).thenReturn(product(5L));
        when(orderItemMapper.countUnfinishedByProduct(5L)).thenReturn(0L);

        service.remove(5L);

        verify(itemMapper).logicDeleteByProduct(eq(5L), any(LocalDateTime.class));
        verify(productMapper).deleteById(5L);
    }

    @Test
    void 删除不存在的商品返回四百() {
        when(productMapper.selectById(9L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> service.remove(9L));
        assertEquals(ResultCode.FAIL.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 夹具

    private AdminProductForm form(Long categoryId) {
        AdminProductForm form = new AdminProductForm();
        form.setCategoryId(categoryId);
        form.setName("星际和平水杯");
        form.setStatus(1);
        form.setSkus(List.of(sku(null, "500ml 星白", "59.00", 100, null)));
        return form;
    }

    private AdminSkuForm sku(Long id, String spec, String price, int stock, Integer version) {
        AdminSkuForm sku = new AdminSkuForm();
        sku.setId(id);
        sku.setSpec(spec);
        sku.setPrice(new BigDecimal(price));
        sku.setStock(stock);
        sku.setVersion(version);
        return sku;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setCategoryId(101L);
        product.setName("星际和平水杯");
        product.setStatus(1);
        product.setSales(0);
        return product;
    }

    private Item item(Long id, Long productId, String spec, int version) {
        Item item = new Item();
        item.setId(id);
        item.setProductId(productId);
        item.setSpec(spec);
        item.setPrice(new BigDecimal("59.00"));
        item.setStock(10);
        item.setVersion(version);
        return item;
    }

    private Category level2Category(Long id, Long parentId) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setName("杯壶");
        return category;
    }

    private Category rootCategory(Long id) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(0L);
        category.setName("家居日用");
        return category;
    }
}
