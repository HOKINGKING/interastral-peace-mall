package com.ipc.mall.auth;

import com.ipc.mall.common.util.AuthWebUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安全跳转与 AJAX 判断（PRD §3.1）。
 */
class AuthWebUtilsTest {

    @Test
    void 只允许站内路径() {
        assertEquals("/user/center", AuthWebUtils.safeRedirect("/user/center"));
        assertEquals("/", AuthWebUtils.safeRedirect(null));
        assertEquals("/", AuthWebUtils.safeRedirect(""));
        assertEquals("/", AuthWebUtils.safeRedirect("//evil.example"));
        assertEquals("/", AuthWebUtils.safeRedirect("https://evil.example"));
        assertEquals("/", AuthWebUtils.safeRedirect("javascript:alert(1)"));
        assertEquals("/", AuthWebUtils.safeRedirect("/a\\b"));
        assertEquals("/", AuthWebUtils.safeRedirect("/a\r\nLocation: //evil"));
    }

    @Test
    void 识别Ajax请求() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertFalse(AuthWebUtils.isAjax(request));

        request.addHeader("X-Requested-With", "XMLHttpRequest");
        assertTrue(AuthWebUtils.isAjax(request));

        MockHttpServletRequest jsonRequest = new MockHttpServletRequest();
        jsonRequest.addHeader("Accept", "application/json, text/plain");
        assertTrue(AuthWebUtils.isAjax(jsonRequest));
    }

    @Test
    void 提取当前路径并编码登录地址() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/order/list");
        request.setQueryString("page=2");

        assertEquals("/order/list?page=2", AuthWebUtils.currentPath(request));
        assertEquals("/login?redirect=%2Forder%2Flist%3Fpage%3D2", AuthWebUtils.loginUrl(request));
    }
}
