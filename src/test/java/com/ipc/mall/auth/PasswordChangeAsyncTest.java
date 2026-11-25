package com.ipc.mall.auth;

import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.front.AccountController;
import com.ipc.mall.dto.ChangePasswordForm;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 修改密码异步接口：错误字段定位、原密码错误、成功跳转与清会话。
 */
class PasswordChangeAsyncTest {

    private AccountService accountService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        AccountController controller = new AccountController(accountService, mock(CaptchaService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void 未登录返回401() throws Exception {
        mockMvc.perform(post("/user/password/async")
                        .param("oldPassword", "123456")
                        .param("newPassword", "abc12345")
                        .param("confirmPassword", "abc12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void 新密码格式错误定位到新密码字段() throws Exception {
        mockMvc.perform(post("/user/password/async")
                        .session(session())
                        .param("oldPassword", "123456")
                        .param("newPassword", "abcdef")
                        .param("confirmPassword", "abcdef"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.FAIL.getCode()))
                .andExpect(jsonPath("$.data.field").value("newPassword"));
    }

    @Test
    void 两次输入不一致定位到确认字段() throws Exception {
        mockMvc.perform(post("/user/password/async")
                        .session(session())
                        .param("oldPassword", "123456")
                        .param("newPassword", "abc12345")
                        .param("confirmPassword", "abc12346"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.field").value("confirmPassword"));
    }

    @Test
    void 原密码错误定位到原密码字段() throws Exception {
        doThrow(new BizException(ResultCode.OLD_PASSWORD_ERROR))
                .when(accountService).changePassword(eq(1L), any(ChangePasswordForm.class));

        mockMvc.perform(post("/user/password/async")
                        .session(session())
                        .param("oldPassword", "wrong123")
                        .param("newPassword", "abc12345")
                        .param("confirmPassword", "abc12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.OLD_PASSWORD_ERROR.getCode()))
                .andExpect(jsonPath("$.data.field").value("oldPassword"));
    }

    @Test
    void 修改成功返回登录跳转并清前台会话() throws Exception {
        doNothing().when(accountService).changePassword(eq(1L), any(ChangePasswordForm.class));
        MockHttpSession session = session();

        mockMvc.perform(post("/user/password/async")
                        .session(session)
                        .param("oldPassword", "123456")
                        .param("newPassword", "abc12345")
                        .param("confirmPassword", "abc12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.redirect").value("/login?changed=1"));

        assertNull(session.getAttribute(SessionKey.LOGIN_ACCOUNT), "改密成功后应清前台登录态");
    }

    private MockHttpSession session() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ACCOUNT, new LoginAccount(1L, "xingye", "星野"));
        return session;
    }
}
