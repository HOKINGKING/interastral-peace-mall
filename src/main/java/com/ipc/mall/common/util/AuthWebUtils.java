package com.ipc.mall.common.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 认证相关的 Web 小工具：安全跳转、AJAX 判断、当前路径提取。
 */
public final class AuthWebUtils {

    /** 形如 http:、https:、javascript: 的 scheme，禁止作为站内跳转目标 */
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*");

    private AuthWebUtils() {
    }

    /**
     * 只允许站内绝对路径，防止开放重定向。
     * 非法值一律回首页，绝不把用户输入直接塞进 sendRedirect。
     */
    public static String safeRedirect(String target) {
        if (target == null) {
            return "/";
        }
        String value = target.trim();
        if (value.isEmpty()
                || !value.startsWith("/")
                || value.startsWith("//")
                || value.contains("\\")
                || value.contains("\r")
                || value.contains("\n")
                || SCHEME_PATTERN.matcher(value).matches()) {
            return "/";
        }
        return value;
    }

    /**
     * 是否为 AJAX/JSON 请求（PRD §3.1）。
     * 不能仅凭浏览器默认的 Accept 通配符判断，必须命中 XMLHttpRequest 或 application/json。
     */
    public static boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    /** 当前请求路径（去掉 contextPath，带 query），用于登录后的 redirect 参数 */
    public static String currentPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
        if (path.isEmpty()) {
            path = "/";
        }
        String query = request.getQueryString();
        return query == null || query.isEmpty() ? path : path + "?" + query;
    }

    /** 构造登录页地址，并对 redirect 参数编码 */
    public static String loginUrl(HttpServletRequest request) {
        return "/login?redirect=" + URLEncoder.encode(currentPath(request), StandardCharsets.UTF_8);
    }

    /** 构造后台登录页地址 */
    public static String adminLoginUrl(HttpServletRequest request) {
        return "/admin/login?redirect=" + URLEncoder.encode(currentPath(request), StandardCharsets.UTF_8);
    }
}
