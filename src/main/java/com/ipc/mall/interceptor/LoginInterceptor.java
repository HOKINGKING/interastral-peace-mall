package com.ipc.mall.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.common.util.AuthWebUtils;
import com.ipc.mall.entity.Account;
import com.ipc.mall.mapper.AccountMapper;
import com.ipc.mall.vo.LoginAccount;
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
 * 前台登录拦截器（PRD §3.1）。
 * <p>
 * 页面请求 302 到登录页并带 redirect；AJAX 请求返回 401 JSON。
 * 已登录用户每 5 分钟回查一次状态，后台禁用后最迟 5 分钟失效。
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    /** 禁用的账号最长 5 分钟后被拦下（PRD §3.1 的 P1 规则） */
    private static final long STATUS_CHECK_INTERVAL_MILLIS = 5 * 60 * 1000L;

    private final AccountMapper accountMapper;
    private final ObjectMapper objectMapper;

    public LoginInterceptor(AccountMapper accountMapper, ObjectMapper objectMapper) {
        this.accountMapper = accountMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(SessionKey.LOGIN_ACCOUNT);
        if (!(value instanceof LoginAccount loginAccount)) {
            reject(request, response);
            return false;
        }

        if (needStatusCheck(session)) {
            Account account = accountMapper.selectById(loginAccount.getAccountId());
            if (account == null || !AccountStatus.isEnabled(account.getStatus())) {
                session.removeAttribute(SessionKey.LOGIN_ACCOUNT);
                session.removeAttribute(SessionKey.LOGIN_ACCOUNT_CHECK_AT);
                reject(request, response);
                return false;
            }
            session.setAttribute(SessionKey.LOGIN_ACCOUNT_CHECK_AT, System.currentTimeMillis());
        }
        return true;
    }

    private boolean needStatusCheck(HttpSession session) {
        Object checked = session.getAttribute(SessionKey.LOGIN_ACCOUNT_CHECK_AT);
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
        String target = AuthWebUtils.loginUrl(request);
        log.debug("未登录访问受保护页面，跳转: {}", target);
        response.sendRedirect(target);
    }
}
