package com.ipc.mall.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 操作日志的跨层上下文（PRD §6.7）。
 * <p>
 * 登录接口的失败不会抛出异常（Controller 捕获后回填错误、渲染登录页），
 * 切面拿不到异常就无法判断成功与否。于是由 Controller 在校验失败时调用
 * {@link #markFailure(HttpServletRequest, String)}，切面记录完成后再清理，
 * 这样“登录失败也记 success=false”才成立。
 */
public final class OperLogContext {

    private static final String FAILURE_ATTRIBUTE = OperLogContext.class.getName() + ".FAILURE";

    private OperLogContext() {
    }

    /** 标记本次请求的业务处理失败（不影响主流程） */
    public static void markFailure(HttpServletRequest request, String message) {
        if (request != null) {
            request.setAttribute(FAILURE_ATTRIBUTE, StringUtils.hasText(message) ? message : "操作失败");
        }
    }

    /** 切面读取失败原因；未标记返回 null，表示按“未抛异常即成功”处理 */
    public static String failureOf(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(FAILURE_ATTRIBUTE);
        return value instanceof String message ? message : null;
    }

    /** 记录完成后清理，避免同一请求内多次记录互相污染 */
    public static void clear(HttpServletRequest request) {
        if (request != null) {
            request.removeAttribute(FAILURE_ATTRIBUTE);
        }
    }
}
