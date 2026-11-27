package com.ipc.mall.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.admin.AdminAccountController;
import com.ipc.mall.dto.AdminAccountForm;
import com.ipc.mall.service.AccountAdminService;
import com.ipc.mall.vo.AdminAccountDetailVO;
import com.ipc.mall.vo.AdminAccountRowVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 后台用户管理接口（A18–A21）：列表筛选、详情回跳、编辑校验、重置密码。
 */
class AdminAccountControllerTest {

    private AccountAdminService accountAdminService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accountAdminService = mock(AccountAdminService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminAccountController(accountAdminService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ------------------------------------------------------------ A18 列表

    @Test
    void 用户列表渲染() throws Exception {
        Page<AdminAccountRowVO> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(row(1L, "xingye")));
        when(accountAdminService.page(null, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/admin/account"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/account/list"))
                .andExpect(model().attribute("pageTitle", "用户管理"))
                .andExpect(model().attribute("total", 1L))
                .andExpect(model().attributeExists("accounts", "pagination"));
    }

    @Test
    void 用户列表按关键词与状态筛选() throws Exception {
        when(accountAdminService.page(any(), any(), anyInt(), anyInt()))
                .thenReturn(new Page<>(1, 10, 0));

        mockMvc.perform(get("/admin/account")
                        .param("keyword", "xingye")
                        .param("status", "0"))
                .andExpect(status().isOk());

        verify(accountAdminService).page(eq("xingye"), eq(0), eq(1), eq(10));
    }

    @Test
    void 用户列表非法状态按全部处理() throws Exception {
        when(accountAdminService.page(any(), any(), anyInt(), anyInt()))
                .thenReturn(new Page<>(1, 10, 0));

        mockMvc.perform(get("/admin/account").param("status", "9"))
                .andExpect(status().isOk());

        verify(accountAdminService).page(any(), isNull(), eq(1), eq(10));
    }

    // ------------------------------------------------------------ A19 详情

    @Test
    void 用户详情渲染() throws Exception {
        AdminAccountDetailVO detail = new AdminAccountDetailVO();
        detail.setId(2L);
        detail.setUsername("xingye");
        detail.setNickname("星野");
        detail.setStatus(1);
        detail.setOrderCount(3L);
        when(accountAdminService.detail(2L)).thenReturn(detail);

        mockMvc.perform(get("/admin/account/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/account/detail"))
                .andExpect(model().attribute("pageTitle", "用户详情"))
                .andExpect(model().attribute("account", detail));
    }

    @Test
    void 用户不存在回列表并提示() throws Exception {
        when(accountAdminService.detail(9L))
                .thenThrow(new BizException(ResultCode.FAIL.getCode(), "用户不存在或已被删除"));

        mockMvc.perform(get("/admin/account/9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/account"))
                .andExpect(flash().attribute("listMessage", "用户不存在或已被删除"));
    }

    // ------------------------------------------------------------ A20 保存

    @Test
    void 保存用户信息成功() throws Exception {
        mockMvc.perform(post("/admin/account/save")
                        .param("id", "1")
                        .param("nickname", "星野新")
                        .param("phone", "13800000002")
                        .param("status", "1"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("用户信息已更新"));

        verify(accountAdminService).save(any(AdminAccountForm.class));
    }

    @Test
    void 保存时忽略adminjs补充的unknown参数() throws Exception {
        // admin.js 会给页面上所有 [data-admin-form] 补一个 visible 字段，绑定层必须忽略它
        mockMvc.perform(post("/admin/account/save")
                        .param("id", "1")
                        .param("nickname", "星野")
                        .param("phone", "13800000001")
                        .param("status", "1")
                        .param("visible", "0"))
                .andExpect(jsonPath("$.code").value(200));

        verify(accountAdminService).save(any(AdminAccountForm.class));
    }

    @Test
    void 保存时手机号非法返回四百() throws Exception {
        mockMvc.perform(post("/admin/account/save")
                        .param("id", "1")
                        .param("nickname", "星野")
                        .param("phone", "123")
                        .param("status", "1"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("手机号需为 11 位数字"));

        verify(accountAdminService, never()).save(any());
    }

    @Test
    void 保存时状态越界返回四百() throws Exception {
        mockMvc.perform(post("/admin/account/save")
                        .param("id", "1")
                        .param("status", "5"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("账号状态只能是 0 或 1"));

        verify(accountAdminService, never()).save(any());
    }

    @Test
    void 保存时缺少用户id返回四百() throws Exception {
        mockMvc.perform(post("/admin/account/save")
                        .param("status", "1"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("缺少用户 id"));

        verify(accountAdminService, never()).save(any());
    }

    // ------------------------------------------------------------ A21 重置密码

    @Test
    void 重置密码成功() throws Exception {
        mockMvc.perform(post("/admin/account/1/reset-password")
                        .param("password", "abc123"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("密码已重置，请通知用户使用新密码登录"));

        verify(accountAdminService).resetPassword(1L, "abc123");
    }

    @Test
    void 重置密码强度不足返回四百() throws Exception {
        mockMvc.perform(post("/admin/account/1/reset-password")
                        .param("password", "123"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("密码需为 6–20 位且同时包含字母和数字"));

        verify(accountAdminService, never()).resetPassword(any(), any());
    }

    // ------------------------------------------------------------ 夹具

    private AdminAccountRowVO row(Long id, String username) {
        AdminAccountRowVO vo = new AdminAccountRowVO();
        vo.setId(id);
        vo.setUsername(username);
        vo.setNickname("星野");
        vo.setPhoneMasked("138****0001");
        vo.setStatus(1);
        return vo;
    }
}
