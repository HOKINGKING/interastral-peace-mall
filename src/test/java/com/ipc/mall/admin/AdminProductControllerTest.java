package com.ipc.mall.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.admin.AdminProductController;
import com.ipc.mall.dto.AdminProductForm;
import com.ipc.mall.dto.SpecPair;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Product;
import com.ipc.mall.service.CategoryService;
import com.ipc.mall.service.ProductAdminService;
import com.ipc.mall.vo.AdminProductRowVO;
import com.ipc.mall.vo.CategoryGroupVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 后台商品管理接口（A9–A13）：列表/表单渲染、JSON 保存校验、上下架、删除限制。
 */
class AdminProductControllerTest {

    private ProductAdminService productAdminService;
    private CategoryService categoryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productAdminService = mock(ProductAdminService.class);
        categoryService = mock(CategoryService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminProductController(productAdminService, categoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ------------------------------------------------------------ A9 列表

    @Test
    void 商品列表渲染() throws Exception {
        Page<AdminProductRowVO> page = new Page<>(1, 10, 10);
        page.setRecords(List.of(row(1L, "星际和平水杯")));
        when(categoryService.allGroups()).thenReturn(List.of());
        when(productAdminService.page(isNull(), isNull(), isNull(), eq(1), eq(10))).thenReturn(page);

        mockMvc.perform(get("/admin/product"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product/list"))
                .andExpect(model().attribute("pageTitle", "商品管理"))
                .andExpect(model().attribute("total", 10L))
                .andExpect(model().attributeExists("products", "pagination", "groups"));
    }

    @Test
    void 按一级分类筛选时展开其子分类() throws Exception {
        CategoryGroupVO group = group(1L, "家居日用", 101L, "杯壶", 102L, "摆件");
        when(categoryService.allGroups()).thenReturn(List.of(group));
        when(productAdminService.page(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new Page<>(1, 10, 0));

        mockMvc.perform(get("/admin/product").param("categoryId", "1"))
                .andExpect(status().isOk());

        verify(productAdminService).page(isNull(), eq(List.of(1L, 101L, 102L)), isNull(), eq(1), eq(10));
    }

    @Test
    void 按二级分类筛选时只查自身() throws Exception {
        when(categoryService.allGroups()).thenReturn(List.of(group(1L, "家居日用", 101L, "杯壶")));
        when(productAdminService.page(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new Page<>(1, 10, 0));

        mockMvc.perform(get("/admin/product").param("categoryId", "101"))
                .andExpect(status().isOk());

        verify(productAdminService).page(isNull(), eq(List.of(101L)), isNull(), eq(1), eq(10));
    }

    // ------------------------------------------------------------ A10 表单

    @Test
    void 新增商品表单() throws Exception {
        when(categoryService.allGroups()).thenReturn(List.of());

        mockMvc.perform(get("/admin/product/form"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product/form"))
                .andExpect(model().attribute("pageTitle", "新增商品"))
                .andExpect(model().attributeDoesNotExist("product"))
                .andExpect(model().attributeExists("items", "specs", "gallery", "groups"));
    }

    @Test
    void 编辑商品表单回填参数与相册() throws Exception {
        Product product = product(5L);
        Item sku = new Item();
        sku.setId(10L);
        sku.setProductId(5L);
        sku.setSpec("500ml 星白");
        sku.setPrice(new BigDecimal("59.00"));
        sku.setStock(100);
        sku.setVersion(2);
        when(productAdminService.get(5L)).thenReturn(Optional.of(product));
        when(productAdminService.itemsOf(5L)).thenReturn(List.of(sku));
        when(productAdminService.specPairsOf(product)).thenReturn(List.of(new SpecPair("材质", "316")));
        when(productAdminService.galleryOf(product)).thenReturn(List.of("/uploads/a.png"));
        when(categoryService.allGroups()).thenReturn(List.of());

        mockMvc.perform(get("/admin/product/form/5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pageTitle", "编辑商品"))
                .andExpect(model().attributeExists("product", "items", "specs", "gallery"));
    }

    @Test
    void 编辑不存在的商品回列表() throws Exception {
        when(productAdminService.get(9L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/product/form/9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/product"))
                .andExpect(flash().attributeExists("listMessage"));
    }

    // ------------------------------------------------------------ A11 保存

    @Test
    void 保存商品成功() throws Exception {
        String body = """
                {"categoryId":101,"name":"新商品","status":1,
                 "specs":[{"key":"材质","value":"316"}],
                 "skus":[{"spec":"标准","price":59.00,"stock":10}]}
                """;

        mockMvc.perform(post("/admin/product/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("商品已创建"));

        verify(productAdminService).save(any(AdminProductForm.class));
    }

    @Test
    void 商品名为空返回四百() throws Exception {
        String body = """
                {"categoryId":101,"name":"","skus":[{"spec":"标准","price":59.00,"stock":10}]}
                """;

        mockMvc.perform(post("/admin/product/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请输入商品名称"));

        verify(productAdminService, never()).save(any());
    }

    @Test
    void 没有SKU时返回四百() throws Exception {
        String body = """
                {"categoryId":101,"name":"无 SKU 商品","skus":[]}
                """;

        mockMvc.perform(post("/admin/product/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("至少保留一个 SKU"));
    }

    @Test
    void 分类不是二级返回四百() throws Exception {
        doThrow(new BizException(ResultCode.FAIL.getCode(), "所属分类必须是二级分类"))
                .when(productAdminService).save(any(AdminProductForm.class));

        String body = """
                {"categoryId":1,"name":"商品","skus":[{"spec":"标准","price":59.00,"stock":10}]}
                """;

        mockMvc.perform(post("/admin/product/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("所属分类必须是二级分类"));
    }

    // ------------------------------------------------------------ A12 / A13

    @Test
    void 上下架返回成功() throws Exception {
        mockMvc.perform(post("/admin/product/5/status").param("status", "0"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("商品已下架"));

        verify(productAdminService).updateStatus(5L, 0);
    }

    @Test
    void 删除商品成功() throws Exception {
        mockMvc.perform(delete("/admin/product/5"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("商品已删除"));

        verify(productAdminService).remove(5L);
    }

    @Test
    void 删除有未完结订单的商品返回两千零五() throws Exception {
        doThrow(new BizException(ResultCode.PRODUCT_IN_USE.getCode(), "该商品还有 2 条未完结订单，无法删除"))
                .when(productAdminService).remove(5L);

        mockMvc.perform(delete("/admin/product/5"))
                .andExpect(jsonPath("$.code").value(2005))
                .andExpect(jsonPath("$.message").value("该商品还有 2 条未完结订单，无法删除"));
    }

    // ------------------------------------------------------------ 夹具

    private AdminProductRowVO row(Long id, String name) {
        AdminProductRowVO vo = new AdminProductRowVO();
        vo.setId(id);
        vo.setName(name);
        vo.setCategoryName("家居日用 / 杯壶");
        vo.setMinPrice(new BigDecimal("59.00"));
        vo.setSales(326);
        vo.setSkuCount(2);
        vo.setTotalStock(128);
        vo.setStatus(1);
        return vo;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setCategoryId(101L);
        product.setName("星际和平水杯");
        product.setStatus(1);
        return product;
    }

    private CategoryGroupVO group(Long rootId, String rootName, Object... childPairs) {
        com.ipc.mall.entity.Category root = new com.ipc.mall.entity.Category();
        root.setId(rootId);
        root.setParentId(0L);
        root.setName(rootName);
        List<com.ipc.mall.entity.Category> children = new java.util.ArrayList<>();
        for (int i = 0; i < childPairs.length; i += 2) {
            com.ipc.mall.entity.Category child = new com.ipc.mall.entity.Category();
            child.setId((Long) childPairs[i]);
            child.setParentId(rootId);
            child.setName((String) childPairs[i + 1]);
            children.add(child);
        }
        return new CategoryGroupVO(root, children);
    }
}
