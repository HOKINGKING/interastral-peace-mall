package com.ipc.mall.admin;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.CategoryForm;
import com.ipc.mall.entity.Category;
import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.CategoryMapper;
import com.ipc.mall.mapper.ProductMapper;
import com.ipc.mall.service.impl.CategoryAdminServiceImpl;
import com.ipc.mall.vo.AdminCategoryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 后台分类管理（PRD §6.2）。
 * <p>
 * 重点：两级结构校验、同层重名、有子分类/商品时删除返回 2004。
 */
class CategoryAdminServiceImplTest {

    private CategoryMapper categoryMapper;
    private ProductMapper productMapper;
    private CategoryAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        categoryMapper = mock(CategoryMapper.class);
        productMapper = mock(ProductMapper.class);
        service = new CategoryAdminServiceImpl(categoryMapper, productMapper);
    }

    // ------------------------------------------------------------ 列表

    @Test
    void 列表按一级与其二级顺序拍平并统计商品数() {
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                category(101L, 1L, "杯壶", 10, 1),
                category(1L, 0L, "家居日用", 10, 1),
                category(2L, 0L, "文创周边", 20, 1),
                category(102L, 1L, "摆件", 20, 1)));
        when(productMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                product(1L, 101L), product(2L, 101L), product(3L, 1L)));

        List<AdminCategoryVO> rows = service.tree(null);

        assertEquals(4, rows.size());
        assertEquals("家居日用", rows.get(0).getName());
        assertTrue(rows.get(0).isRoot());
        assertEquals(2, rows.get(0).getChildCount());
        assertEquals(1, rows.get(0).getProductCount());

        assertEquals("杯壶", rows.get(1).getName());
        assertFalse(rows.get(1).isRoot());
        assertEquals("家居日用", rows.get(1).getParentName());
        assertEquals(2, rows.get(1).getProductCount());

        assertEquals("摆件", rows.get(2).getName());
        assertEquals(0, rows.get(2).getProductCount());

        assertEquals("文创周边", rows.get(3).getName());
        assertTrue(rows.get(3).isRoot());
        assertTrue(rows.get(3).isVisible());
    }

    @Test
    void 关键词命中二级时带上其上级作为上下文() {
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                category(1L, 0L, "家居日用", 10, 1),
                category(101L, 1L, "杯壶", 10, 1),
                category(102L, 1L, "摆件", 20, 1)));
        when(productMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        List<AdminCategoryVO> rows = service.tree("杯");

        assertEquals(2, rows.size());
        assertEquals("家居日用", rows.get(0).getName());
        assertEquals("杯壶", rows.get(1).getName());
    }

    @Test
    void 关键词命中一级时列出它全部二级() {
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                category(1L, 0L, "家居日用", 10, 1),
                category(101L, 1L, "杯壶", 10, 1),
                category(102L, 1L, "摆件", 20, 1),
                category(2L, 0L, "文创周边", 20, 1)));
        when(productMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        List<AdminCategoryVO> rows = service.tree("家居");

        assertEquals(3, rows.size());
        assertEquals("家居日用", rows.get(0).getName());
        assertEquals("杯壶", rows.get(1).getName());
        assertEquals("摆件", rows.get(2).getName());
    }

    // ------------------------------------------------------------ 保存

    @Test
    void 新增一级分类() {
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        service.save(form(null, 0L, "  新品上架  ", "描述", 5, "on"));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).insert(captor.capture());
        Category saved = captor.getValue();
        assertEquals(0L, saved.getParentId());
        assertEquals("新品上架", saved.getName());
        assertEquals(5, saved.getSort());
        assertEquals(1, saved.getVisible());
        assertEquals("描述", saved.getDescn());
    }

    @Test
    void 新增二级分类校验上级必须是一级() {
        when(categoryMapper.selectById(101L)).thenReturn(category(101L, 1L, "杯壶", 10, 1));

        BizException exception = assertThrows(BizException.class,
                () -> service.save(form(null, 101L, "保温杯", null, 1, "1")));

        assertEquals(ResultCode.FAIL.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("一级分类"));
        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    void 新增二级分类成功() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, 0L, "家居日用", 10, 1));
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        service.save(form(null, 1L, "保温杯", null, 2, null));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getParentId());
        assertEquals(2, captor.getValue().getSort());
        assertEquals(0, captor.getValue().getVisible(), "未勾选可见应存 0");
    }

    @Test
    void 同层重名被拒绝() {
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BizException exception = assertThrows(BizException.class,
                () -> service.save(form(null, 0L, "家居日用", null, 1, "1")));

        assertEquals(ResultCode.FAIL.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("同名"));
        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    void 编辑不存在的分类返回四百() {
        when(categoryMapper.selectById(9L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class,
                () -> service.save(form(9L, 0L, "任意", null, 1, "1")));

        assertEquals(ResultCode.FAIL.getCode(), exception.getCode());
        verify(categoryMapper, never()).updateFields(any(Category.class), any(LocalDateTime.class));
    }

    @Test
    void 有子分类的一级不能改成二级() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, 0L, "家居日用", 10, 1));
        when(categoryMapper.selectById(2L)).thenReturn(category(2L, 0L, "文创周边", 20, 1));
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        BizException exception = assertThrows(BizException.class,
                () -> service.save(form(1L, 2L, "家居日用", null, 10, "1")));

        assertTrue(exception.getMessage().contains("子分类"));
        verify(categoryMapper, never()).updateFields(any(Category.class), any(LocalDateTime.class));
    }

    @Test
    void 编辑成功走整行覆盖更新() {
        when(categoryMapper.selectById(101L)).thenReturn(category(101L, 1L, "杯壶", 10, 1));
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, 0L, "家居日用", 10, 1));
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(categoryMapper.updateFields(any(Category.class), any(LocalDateTime.class))).thenReturn(1);

        service.save(form(101L, 1L, "水杯", "新描述", 30, "0"));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).updateFields(captor.capture(), any(LocalDateTime.class));
        assertEquals(101L, captor.getValue().getId());
        assertEquals("水杯", captor.getValue().getName());
        assertEquals(0, captor.getValue().getVisible());
        verify(categoryMapper, never()).insert(any(Category.class));
    }

    // ------------------------------------------------------------ 删除

    @Test
    void 有子分类的分类不能删除() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, 0L, "家居日用", 10, 1));
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(2L);

        BizException exception = assertThrows(BizException.class, () -> service.remove(1L));

        assertEquals(ResultCode.CATEGORY_IN_USE.getCode(), exception.getCode());
        verify(categoryMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void 有商品的分类不能删除() {
        when(categoryMapper.selectById(101L)).thenReturn(category(101L, 1L, "杯壶", 10, 1));
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(3L);

        BizException exception = assertThrows(BizException.class, () -> service.remove(101L));

        assertEquals(ResultCode.CATEGORY_IN_USE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("3 个商品"));
        verify(categoryMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void 空分类可以逻辑删除() {
        when(categoryMapper.selectById(601L)).thenReturn(category(601L, 0L, "节日礼盒", 60, 1));
        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(productMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        service.remove(601L);

        verify(categoryMapper).deleteById(601L);
    }

    @Test
    void 删除不存在的分类返回四百() {
        when(categoryMapper.selectById(9L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> service.remove(9L));

        assertEquals(ResultCode.FAIL.getCode(), exception.getCode());
        verify(categoryMapper, never()).deleteById(any(Long.class));
    }

    // ------------------------------------------------------------ 读取辅助

    @Test
    void 查询与是否存在子分类() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, 0L, "家居日用", 10, 1));
        when(categoryMapper.selectById(9L)).thenReturn(null);

        assertEquals(Optional.of(1L), service.get(1L).map(Category::getId));
        assertTrue(service.get(9L).isEmpty());
        assertTrue(service.get(null).isEmpty());

        when(categoryMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertTrue(service.hasChildren(1L));
        assertFalse(service.hasChildren(null));
    }

    @Test
    void 一级分类下拉按排序返回() {
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                category(1L, 0L, "家居日用", 10, 1), category(2L, 0L, "文创周边", 20, 1)));

        List<Category> roots = service.roots();

        assertEquals(2, roots.size());
        assertEquals("家居日用", roots.get(0).getName());
    }

    // ------------------------------------------------------------ 夹具

    private CategoryForm form(Long id, Long parentId, String name, String descn, int sort, String visible) {
        CategoryForm form = new CategoryForm();
        form.setId(id);
        form.setParentId(parentId);
        form.setName(name);
        form.setDescn(descn);
        form.setSort(sort);
        form.setVisible(visible);
        return form;
    }

    private Category category(Long id, Long parentId, String name, int sort, int visible) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setSort(sort);
        category.setVisible(visible);
        category.setDescn(name + "描述");
        return category;
    }

    private Product product(Long id, Long categoryId) {
        Product product = new Product();
        product.setId(id);
        product.setCategoryId(categoryId);
        return product;
    }
}
