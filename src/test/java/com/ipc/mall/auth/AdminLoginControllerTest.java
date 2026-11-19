package com.ipc.mall.auth;

import com.ipc.mall.common.constant.CaptchaScope;
import com.ipc.mall.controller.admin.AdminLoginController;
import com.ipc.mall.service.AdminService;
import com.ipc.mall.service.CaptchaService;
import com.ipc.mall.vo.LoginAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 后台登录流程（PRD §6.1）。
 */
class AdminLoginControllerTest {

    private AdminService adminService;
    private CaptchaService captchaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminService = mock(AdminService.class);
        captchaService = mock(CaptchaService.class);
        AdminLoginController controller = new AdminLoginController(adminService, captchaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void 后台登录页可访问() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"))
                .andExpect(model().attributeExists("adminLoginForm"));
    }

    @Test
    void 后台登录成功跳工作台() throws Exception {
        when(captchaService.verify(any(), eq(CaptchaScope.ADMIN), eq("ABCD"))).thenReturn(true);
        when(adminService.login("admin", "123456"))
                .thenReturn(new LoginAdmin(1L, "admin", "平台管理员"));

        mockMvc.perform(post("/admin/login")
                        .param("username", "admin")
                        .param("password", "123456")
                        .param("captcha", "ABCD")
                        .session(new MockHttpSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void 后台登录失败回显错误() throws Exception {
        when(captchaService.verify(any(), eq(CaptchaScope.ADMIN), eq("ABCD"))).thenReturn(true);
        when(adminService.login("admin", "wrong"))
                .thenThrow(new com.ipc.mall.common.exception.BizException(
                        com.ipc.mall.common.result.ResultCode.ADMIN_LOGIN_FAILED));

        mockMvc.perform(post("/admin/login")
                        .param("username", "admin")
                        .param("password", "wrong")
                        .param("captcha", "ABCD")
                        .session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"))
                .andExpect(model().attributeHasErrors("adminLoginForm"));
    }
}
