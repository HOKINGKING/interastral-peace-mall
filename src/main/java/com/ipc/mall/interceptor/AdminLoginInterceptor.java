package com.ipc.mall.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.common.util.AuthWebUtils;
import com.ipc.mall.entity.Admin;
import com.ipc.mall.mapper.AdminMapper;
import com.ipc.mall.vo.LoginAdmin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 后台登录拦截器（PRD §3.1）：只认 LOGIN_ADMIN，不触碰前台 key。
 */
@Slf4j
@Component
public class AdminLoginInterceptor implements HandlerInterceptor {

    private static final long STATUS_CHECK_INTERVAL_MILLIS = 5 * 60 * 1000L;

    private final AdminMapper adminMapper;
    private final ObjectMapper objectMapper;

    public AdminLoginInterceptor(AdminMapper adminMapper, ObjectMapper objectMapper) {
        this.adminMapper = adminMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(SessionKey.LOGIN_ADMIN);
        if (!(value instanceof LoginAdmin loginAdmin)) {
            reject(request, response);
            return false;
        }

        if (needStatusCheck(session)) {
            Admin admin = adminMapper.selectById(loginAdmin.getAdminId());
            if (admin == null || !AccountStatus.isEnabled(admin.getStatus())) {
                session.removeAttribute(SessionKey.LOGIN_ADMIN);
                session.removeAttribute(SessionKey.LOGIN_ADMIN_CHECK_AT);
                reject(request, response);
                return false;
            }
            session.setAttribute(SessionKey.LOGIN_ADMIN_CHECK_AT, System.currentTimeMillis());
        }
        return true;
    }

    private boolean needStatusCheck(HttpSession session) {
        Object checked = session.getAttribute(SessionKey.LOGIN_ADMIN_CHECK_AT);
        if (!(checked instanceof Number number)) {
            return true;
        }
        return System.currentTimeMillis() - number.longValue() > STATUS_CHECK_INTERVAL_MILLIS;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (AuthWebUtils.isAjax(request)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), Result.fail(ResultCode.UNAUTHORIZED));
            return;
        }
        String target = AuthWebUtils.adminLoginUrl(request);
        log.debug("未登录访问后台页面，跳转: {}", target);
        response.sendRedirect(target);
    }
}
