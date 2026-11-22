package com.ipc.mall.address;

import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.front.AddressController;
import com.ipc.mall.dto.AddressForm;
import com.ipc.mall.service.AddressService;
import com.ipc.mall.vo.AddressVO;
import com.ipc.mall.vo.LoginAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
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
 * 收货地址页面与接口（F19–F23）。
 */
class AddressControllerTest {

    private AddressService addressService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        addressService = mock(AddressService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AddressController(addressService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ------------------------------------------------------------ F19 列表

    @Test
    void 未登录访问地址列表跳登录() throws Exception {
        mockMvc.perform(get("/address/list"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void 地址列表渲染卡片与上限() throws Exception {
        AddressVO vo = new AddressVO();
        vo.setId(2L);
        vo.setReceiver("星野");
        vo.setPhoneMasked("138****0001");
        vo.setFullAddress("湖南省 长沙市 岳麓区 星轨大道 88 号");
        vo.setDefaultFlag(true);
        when(addressService.list(1L)).thenReturn(List.of(vo));

        mockMvc.perform(get("/address/list").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/address/list"))
                .andExpect(model().attribute("pageTitle", "收货地址"))
                .andExpect(model().attribute("limit", AddressService.MAX_COUNT))
                .andExpect(model().attribute("addresses", List.of(vo)));
    }

    // ------------------------------------------------------------ F20 表单

    @Test
    void 新增表单是空表单() throws Exception {
        mockMvc.perform(get("/address/form").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/address/form"))
                .andExpect(model().attribute("pageTitle", "新增收货地址"))
                .andExpect(model().attributeExists("addressForm"));
    }

    @Test
    void 编辑表单回填自己的地址() throws Exception {
        when(addressService.get(1L, 2L)).thenReturn(Optional.of(address(2L, 1L)));

        mockMvc.perform(get("/address/form/2").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/address/form"))
                .andExpect(model().attribute("pageTitle", "编辑收货地址"))
                .andExpect(model().attribute("addressForm", hasProperty("id", is(2L))))
                .andExpect(model().attribute("addressForm", hasProperty("receiver", is("星野"))))
                .andExpect(model().attribute("addressForm", hasProperty("detail", is("星轨大道 88 号"))))
                .andExpect(model().attribute("addressForm", hasProperty("isDefault", is("1"))));

        verify(addressService).get(1L, 2L);
    }

    @Test
    void 编辑别人的地址回列表并提示() throws Exception {
        when(addressService.get(2L, 1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/address/form/1").session(loginSession(2L, "bob")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/address/list"))
                .andExpect(flash().attribute("listMessage", "收货地址不存在或不属于当前账号"));
    }

    // ------------------------------------------------------------ F21 保存

    @Test
    void 保存地址返回成功() throws Exception {
        mockMvc.perform(post("/address/save")
                        .param("receiver", "星野")
                        .param("phone", "13800000001")
                        .param("province", "湖南省")
                        .param("city", "长沙市")
                        .param("district", "岳麓区")
                        .param("detail", "星轨大道 88 号")
                        .session(loginSession()))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("收货地址已保存"));

        verify(addressService).save(eq(1L), any(AddressForm.class));
    }

    @Test
    void 更新地址返回更新提示() throws Exception {
        mockMvc.perform(post("/address/save")
                        .param("id", "2")
                        .param("receiver", "星野")
                        .param("phone", "13800000001")
                        .param("detail", "新地址 1 号")
                        .session(loginSession()))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("收货地址已更新"));
    }

    @Test
    void 收货人为空返回四百与字段提示() throws Exception {
        mockMvc.perform(post("/address/save")
                        .param("receiver", "   ")
                        .param("phone", "13800000001")
                        .param("detail", "某地")
                        .session(loginSession()))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请输入收货人"));

        verify(addressService, never()).save(any(), any());
    }

    @Test
    void 手机号格式错误返回四百与字段提示() throws Exception {
        mockMvc.perform(post("/address/save")
                        .param("receiver", "星野")
                        .param("phone", "123")
                        .param("detail", "某地")
                        .session(loginSession()))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("手机号需为 11 位数字"));
    }

    @Test
    void 未登录保存返回四百零一() throws Exception {
        mockMvc.perform(post("/address/save")
                        .param("receiver", "星野")
                        .param("phone", "13800000001")
                        .param("detail", "某地"))
                .andExpect(jsonPath("$.code").value(ResultCode.UNAUTHORIZED.getCode()));

        verify(addressService, never()).save(any(), any());
    }

    @Test
    void 超过上限时返回四百与提示() throws Exception {
        doThrow(new BizException(ResultCode.FAIL.getCode(), "收货地址最多保存 20 条"))
                .when(addressService).save(eq(1L), any(AddressForm.class));

        mockMvc.perform(post("/address/save")
                        .param("receiver", "星野")
                        .param("phone", "13800000001")
                        .param("detail", "某地")
                        .session(loginSession()))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("收货地址最多保存 20 条"));
    }

    // ------------------------------------------------------------ F22 / F23

    @Test
    void 删除自己的地址返回成功() throws Exception {
        mockMvc.perform(delete("/address/2").session(loginSession()))
                .andExpect(jsonPath("$.code").value(200));

        verify(addressService).remove(1L, 2L);
    }

    @Test
    void 删除别人的地址返回三千零四() throws Exception {
        doThrow(new BizException(ResultCode.NO_PERMISSION.getCode(), "收货地址不存在或不属于当前用户"))
                .when(addressService).remove(2L, 1L);

        mockMvc.perform(delete("/address/1").session(loginSession(2L, "bob")))
                .andExpect(jsonPath("$.code").value(3004));
    }

    @Test
    void 设为默认返回成功() throws Exception {
        mockMvc.perform(post("/address/2/default").session(loginSession()))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已设为默认地址"));

        verify(addressService).setDefault(1L, 2L);
    }

    @Test
    void 未登录设为默认返回四百零一() throws Exception {
        mockMvc.perform(post("/address/2/default"))
                .andExpect(jsonPath("$.code").value(ResultCode.UNAUTHORIZED.getCode()));

        verify(addressService, never()).setDefault(any(), any());
    }

    // ------------------------------------------------------------ 夹具

    private com.ipc.mall.entity.Address address(Long id, Long accountId) {
        com.ipc.mall.entity.Address address = new com.ipc.mall.entity.Address();
        address.setId(id);
        address.setAccountId(accountId);
        address.setReceiver("星野");
        address.setPhone("13800000001");
        address.setProvince("湖南省");
        address.setCity("长沙市");
        address.setDistrict("岳麓区");
        address.setDetail("星轨大道 88 号");
        address.setIsDefault(1);
        return address;
    }

    private MockHttpSession loginSession() {
        return loginSession(1L, "xingye");
    }

    private MockHttpSession loginSession(Long accountId, String username) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ACCOUNT, new LoginAccount(accountId, username, username));
        return session;
    }
}
