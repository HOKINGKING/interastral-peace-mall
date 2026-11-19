package com.ipc.mall.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.entity.Account;
import com.ipc.mall.interceptor.LoginInterceptor;
import com.ipc.mall.mapper.AccountMapper;
import com.ipc.mall.vo.LoginAccount;
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
 * 前台拦截器：页面 302、AJAX 401、5 分钟状态回查。
 */
class LoginInterceptorTest {

    private AccountMapper accountMapper;
    private LoginInterceptor interceptor;
    private HandlerMethod handler;

    @BeforeEach
    void setUp() {
        accountMapper = mock(AccountMapper.class);
        interceptor = new LoginInterceptor(accountMapper, new ObjectMapper());
        handler = mock(HandlerMethod.class);
    }

    @Test
    void 未登录页面跳转登录页() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/center");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, handler));
        assertEquals("/login?redirect=%2Fuser%2Fcenter", response.getRedirectedUrl());
    }

    @Test
    void 未登录Ajax返回401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/center");
        request.addHeader("Accept", "application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, handler));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":401"));
    }

    @Test
    void 已登录且刚回查过直接放行() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/center");
        request.getSession().setAttribute(SessionKey.LOGIN_ACCOUNT, new LoginAccount(1L, "xingye", "星野"));
        request.getSession().setAttribute(SessionKey.LOGIN_ACCOUNT_CHECK_AT, System.currentTimeMillis());

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), handler));
    }

    @Test
    void 回查发现账号被禁用则清会话() throws Exception {
        Account disabled = new Account();
        disabled.setId(1L);
        disabled.setStatus(AccountStatus.DISABLED);
        when(accountMapper.selectById(1L)).thenReturn(disabled);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/center");
        request.getSession().setAttribute(SessionKey.LOGIN_ACCOUNT, new LoginAccount(1L, "xingye", "星野"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, handler));
        assertNull(request.getSession().getAttribute(SessionKey.LOGIN_ACCOUNT));
        assertEquals("/login?redirect=%2Fuser%2Fcenter", response.getRedirectedUrl());
    }
}
