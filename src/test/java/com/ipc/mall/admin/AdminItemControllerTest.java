package com.ipc.mall.admin;

import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.admin.AdminItemController;
import com.ipc.mall.dto.AdminSkuForm;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Product;
import com.ipc.mall.service.ItemAdminService;
import com.ipc.mall.service.ProductAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
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
 * 后台 SKU 管理接口（A14–A16）。
 */
class AdminItemControllerTest {

    private ItemAdminService itemAdminService;
    private ProductAdminService productAdminService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        itemAdminService = mock(ItemAdminService.class);
        productAdminService = mock(ProductAdminService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminItemController(itemAdminService, productAdminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        when(productAdminService.get(5L)).thenReturn(Optional.of(product(5L)));
    }

    @Test
    void SKU列表渲染() throws Exception {
        when(itemAdminService.listByProduct(5L)).thenReturn(List.of(item(10L, "500ml 星白")));

        mockMvc.perform(get("/admin/item").param("productId", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/item/list"))
                .andExpect(model().attribute("pageTitle", "SKU 管理"))
                .andExpect(model().attribute("product", product(5L)))
                .andExpect(model().attributeExists("items"));
    }

    @Test
    void 缺少商品id时回商品列表() throws Exception {
        mockMvc.perform(get("/admin/item"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/product"))
                .andExpect(flash().attributeExists("listMessage"));
    }

    @Test
    void 商品不存在时回商品列表() throws Exception {
        when(productAdminService.get(9L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/item").param("productId", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/product"));
    }

    @Test
    void 新增SKU表单() throws Exception {
        mockMvc.perform(get("/admin/item/form").param("productId", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/item/form"))
                .andExpect(model().attribute("pageTitle", "新增 SKU"))
                .andExpect(model().attribute("skuForm", hasProperty("productId", is(5L))));
    }

    @Test
    void 编辑SKU表单回填版本号() throws Exception {
        when(itemAdminService.get(10L)).thenReturn(Optional.of(item(10L, "500ml 星白")));

        mockMvc.perform(get("/admin/item/form/10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pageTitle", "编辑 SKU"))
                .andExpect(model().attribute("skuForm", hasProperty("spec", is("500ml 星白"))))
                .andExpect(model().attribute("skuForm", hasProperty("version", is(2))));
    }

    @Test
    void 保存SKU成功() throws Exception {
        mockMvc.perform(post("/admin/item/save")
                        .param("productId", "5")
                        .param("spec", "500ml 深空蓝")
                        .param("price", "59.00")
                        .param("stock", "20"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("SKU 已创建"));

        verify(itemAdminService).save(any(AdminSkuForm.class));
    }

    @Test
    void 规格为空返回四百() throws Exception {
        mockMvc.perform(post("/admin/item/save")
                        .param("productId", "5")
                        .param("spec", "  ")
                        .param("price", "59.00")
                        .param("stock", "20"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请输入 SKU 规格"));

        verify(itemAdminService, never()).save(any());
    }

    @Test
    void 价格越界返回四百() throws Exception {
        mockMvc.perform(post("/admin/item/save")
                        .param("productId", "5")
                        .param("spec", "标准")
                        .param("price", "0")
                        .param("stock", "20"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("价格需在 0.01–99999.99 之间"));
    }

    @Test
    void 删除SKU成功() throws Exception {
        mockMvc.perform(delete("/admin/item/10"))
                .andExpect(jsonPath("$.code").value(200));

        verify(itemAdminService).remove(10L);
    }

    @Test
    void 删除最后一条SKU返回四百() throws Exception {
        doThrow(new BizException(ResultCode.FAIL.getCode(), "每个商品至少保留一个 SKU，不能删除最后一条"))
                .when(itemAdminService).remove(10L);

        mockMvc.perform(delete("/admin/item/10"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("每个商品至少保留一个 SKU，不能删除最后一条"));
    }

    // ------------------------------------------------------------ 夹具

    private Item item(Long id, String spec) {
        Item item = new Item();
        item.setId(id);
        item.setProductId(5L);
        item.setSpec(spec);
        item.setPrice(new BigDecimal("59.00"));
        item.setStock(10);
        item.setVersion(2);
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
