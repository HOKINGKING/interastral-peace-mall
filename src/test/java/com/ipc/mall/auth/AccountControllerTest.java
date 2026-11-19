package com.ipc.mall.auth;

import com.ipc.mall.common.constant.CaptchaScope;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.front.AccountController;
import com.ipc.mall.dto.RegisterForm;
import com.ipc.mall.service.AccountService;
import com.ipc.mall.service.CaptchaService;
import com.ipc.mall.vo.LoginAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertNull;
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
 * 前台认证页面流程（PRG、错误回显、退出）。
 */
class AccountControllerTest {

    private AccountService accountService;
    private CaptchaService captchaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        captchaService = mock(CaptchaService.class);
        AccountController controller = new AccountController(accountService, captchaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void 登录页可访问() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("front/login"))
                .andExpect(model().attributeExists("loginForm"));
    }

    @Test
    void 登录成功重定向首页() throws Exception {
        when(captchaService.verify(any(), eq(CaptchaScope.FRONT), eq("ABCD"))).thenReturn(true);
        when(accountService.login("xingye", "PetStore123!"))
                .thenReturn(new LoginAccount(1L, "xingye", "星野"));

        mockMvc.perform(post("/user/login")
                        .param("username", "xingye")
                        .param("password", "PetStore123!")
                        .param("captcha", "ABCD")
                        .session(new MockHttpSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void 验证码错误回显登录页() throws Exception {
        when(captchaService.verify(any(), eq(CaptchaScope.FRONT), eq("WRONG"))).thenReturn(false);

        mockMvc.perform(post("/user/login")
                        .param("username", "xingye")
                        .param("password", "PetStore123!")
                        .param("captcha", "WRONG")
                        .session(new MockHttpSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/login"))
                .andExpect(model().attributeHasFieldErrors("loginForm", "captcha"));
    }

    @Test
    void 注册成功自动登录并跳首页() throws Exception {
        when(captchaService.verify(any(), eq(CaptchaScope.FRONT), eq("ABCD"))).thenReturn(true);
        when(accountService.register(any(RegisterForm.class)))
                .thenReturn(new LoginAccount(9L, "new_user", "new_user"));

        mockMvc.perform(post("/user/register")
                        .param("username", "new_user")
                        .param("password", "Passw0rd")
                        .param("confirmPassword", "Passw0rd")
                        .param("phone", "")
                        .param("captcha", "ABCD")
                        .session(new MockHttpSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void 退出只清前台登录态() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ACCOUNT, new LoginAccount(1L, "xingye", "星野"));
        session.setAttribute(SessionKey.LOGIN_ADMIN, "admin-session");

        mockMvc.perform(post("/user/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        assertNull(session.getAttribute(SessionKey.LOGIN_ACCOUNT));
        // 后台 key 必须保留，验证前后台会话隔离
        org.junit.jupiter.api.Assertions.assertEquals("admin-session",
                session.getAttribute(SessionKey.LOGIN_ADMIN));
    }

    @Test
    void 未登录访问个人中心跳登录() throws Exception {
        mockMvc.perform(get("/user/center"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void 原密码错误回显改密页() throws Exception {
        org.mockito.Mockito.doThrow(new BizException(ResultCode.OLD_PASSWORD_ERROR))
                .when(accountService).changePassword(eq(1L), any());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ACCOUNT, new LoginAccount(1L, "xingye", "星野"));

        mockMvc.perform(post("/user/password")
                        .param("oldPassword", "wrong")
                        .param("newPassword", "NewPass123")
                        .param("confirmPassword", "NewPass123")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("front/user/password"))
                .andExpect(model().attributeHasFieldErrors("changePasswordForm", "oldPassword"));
    }
}
