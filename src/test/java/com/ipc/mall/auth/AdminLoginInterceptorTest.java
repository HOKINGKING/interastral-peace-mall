package com.ipc.mall.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.entity.Admin;
import com.ipc.mall.interceptor.AdminLoginInterceptor;
import com.ipc.mall.mapper.AdminMapper;
import com.ipc.mall.vo.LoginAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 后台拦截器：页面 302、AJAX 401、5 分钟状态回查。
 */
class AdminLoginInterceptorTest {

    private AdminMapper adminMapper;
    private AdminLoginInterceptor interceptor;
    private HandlerMethod handler;

    @BeforeEach
    void setUp() {
        adminMapper = mock(AdminMapper.class);
        interceptor = new AdminLoginInterceptor(adminMapper, new ObjectMapper());
        handler = mock(HandlerMethod.class);
    }

    @Test
    void 未登录页面跳转后台登录页() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, handler));
        assertEquals("/admin/login?redirect=%2Fadmin", response.getRedirectedUrl());
    }

    @Test
    void 未登录Ajax返回401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        request.addHeader("Accept", "application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, handler));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":401"));
    }

    @Test
    void 已登录且刚回查过直接放行() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        request.getSession().setAttribute(SessionKey.LOGIN_ADMIN, new LoginAdmin(1L, "admin", "平台管理员"));
        request.getSession().setAttribute(SessionKey.LOGIN_ADMIN_CHECK_AT, System.currentTimeMillis());

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), handler));
    }

    @Test
    void 回查发现后台账号被禁用则清会话() throws Exception {
        Admin disabled = new Admin();
        disabled.setId(1L);
        disabled.setStatus(AccountStatus.DISABLED);
        when(adminMapper.selectById(1L)).thenReturn(disabled);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin");
        request.getSession().setAttribute(SessionKey.LOGIN_ADMIN, new LoginAdmin(1L, "admin", "平台管理员"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, handler));
        assertNull(request.getSession().getAttribute(SessionKey.LOGIN_ADMIN));
        assertEquals("/admin/login?redirect=%2Fadmin", response.getRedirectedUrl());
    }
}
