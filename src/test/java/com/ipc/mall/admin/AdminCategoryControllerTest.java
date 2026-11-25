package com.ipc.mall.admin;

import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.admin.AdminCategoryController;
import com.ipc.mall.dto.CategoryForm;
import com.ipc.mall.entity.Category;
import com.ipc.mall.service.CategoryAdminService;
import com.ipc.mall.vo.AdminCategoryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
 * 后台分类管理（A5–A8）：列表/表单渲染、保存校验、删除限制。
 * <p>
 * 登录态由 AdminLoginInterceptor 负责，这里用 standalone 只测控制器自身行为；
 * 「未登录 302」在真实 HTTP 验证里覆盖。
 */
class AdminCategoryControllerTest {

    private CategoryAdminService categoryAdminService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        categoryAdminService = mock(CategoryAdminService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCategoryController(categoryAdminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ------------------------------------------------------------ A5 列表

    @Test
    void 分类列表渲染() throws Exception {
        AdminCategoryVO row = new AdminCategoryVO();
        row.setId(1L);
        row.setName("家居日用");
        row.setRoot(true);
        row.setProductCount(1);
        when(categoryAdminService.tree(null)).thenReturn(List.of(row));

        mockMvc.perform(get("/admin/category"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/category/list"))
                .andExpect(model().attribute("pageTitle", "分类管理"))
                .andExpect(model().attribute("categories", List.of(row)));
    }

    @Test
    void 分类列表按关键词搜索() throws Exception {
        when(categoryAdminService.tree("杯")).thenReturn(List.of());

        mockMvc.perform(get("/admin/category").param("keyword", "杯"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("keyword", "杯"));

        verify(categoryAdminService).tree("杯");
    }

    // ------------------------------------------------------------ A6 表单

    @Test
    void 新增分类表单带一级分类下拉() throws Exception {
        when(categoryAdminService.roots()).thenReturn(List.of(category(1L, 0L, "家居日用")));

        mockMvc.perform(get("/admin/category/form"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/category/form"))
                .andExpect(model().attribute("pageTitle", "新增分类"))
                .andExpect(model().attribute("lockedParent", false))
                .andExpect(model().attributeExists("categoryForm", "roots"));
    }

    @Test
    void 编辑一级分类时回填并锁住上级() throws Exception {
        when(categoryAdminService.get(1L)).thenReturn(Optional.of(category(1L, 0L, "家居日用")));
        when(categoryAdminService.hasChildren(1L)).thenReturn(true);
        when(categoryAdminService.roots()).thenReturn(List.of(category(1L, 0L, "家居日用")));

        mockMvc.perform(get("/admin/category/form/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/category/form"))
                .andExpect(model().attribute("pageTitle", "编辑分类"))
                .andExpect(model().attribute("lockedParent", true))
                .andExpect(model().attribute("categoryForm", hasProperty("name", is("家居日用"))))
                .andExpect(model().attribute("categoryForm", hasProperty("parentId", is(0L))));
    }

    @Test
    void 编辑不存在的分类回列表并提示() throws Exception {
        when(categoryAdminService.get(9L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/category/form/9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/category"))
                .andExpect(flash().attributeExists("listMessage"));
    }

    // ------------------------------------------------------------ A7 保存

    @Test
    void 新增分类返回成功() throws Exception {
        mockMvc.perform(post("/admin/category/save")
                        .param("parentId", "0")
                        .param("name", "新品上架")
                        .param("descn", "新分类")
                        .param("sort", "5")
                        .param("visible", "1"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("分类已创建"));

        verify(categoryAdminService).save(any(CategoryForm.class));
    }

    @Test
    void 更新分类返回更新提示() throws Exception {
        mockMvc.perform(post("/admin/category/save")
                        .param("id", "101")
                        .param("parentId", "1")
                        .param("name", "水杯")
                        .param("sort", "10"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("分类已更新"));
    }

    @Test
    void 分类名为空返回四百() throws Exception {
        mockMvc.perform(post("/admin/category/save").param("parentId", "0").param("name", "  "))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请输入分类名称"));

        verify(categoryAdminService, never()).save(any());
    }

    @Test
    void 分类名超长返回四百() throws Exception {
        String longName = "分".repeat(31);

        mockMvc.perform(post("/admin/category/save").param("parentId", "0").param("name", longName))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("分类名称最多 30 个字符"));
    }

    @Test
    void 同层重名返回四百并带原因() throws Exception {
        doThrow(new BizException(ResultCode.FAIL.getCode(), "同一层级下已存在同名分类"))
                .when(categoryAdminService).save(any(CategoryForm.class));

        mockMvc.perform(post("/admin/category/save").param("parentId", "0").param("name", "家居日用"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("同一层级下已存在同名分类"));
    }

    // ------------------------------------------------------------ A8 删除

    @Test
    void 删除分类返回成功() throws Exception {
        mockMvc.perform(delete("/admin/category/601"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("分类已删除"));

        verify(categoryAdminService).remove(601L);
    }

    @Test
    void 删除有子分类或商品返回两千零四() throws Exception {
        doThrow(new BizException(ResultCode.CATEGORY_IN_USE.getCode(), "该分类下还有 3 个商品，无法删除"))
                .when(categoryAdminService).remove(eq(101L));

        mockMvc.perform(delete("/admin/category/101"))
                .andExpect(jsonPath("$.code").value(2004))
                .andExpect(jsonPath("$.message").value("该分类下还有 3 个商品，无法删除"));
    }

    // ------------------------------------------------------------ 夹具

    private Category category(Long id, Long parentId, String name) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setSort(10);
        category.setVisible(1);
        return category;
    }
}
