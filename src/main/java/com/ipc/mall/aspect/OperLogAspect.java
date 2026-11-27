package com.ipc.mall.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipc.mall.annotation.OperLog;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.util.OperLogContext;
import com.ipc.mall.entity.SysOperLog;
import com.ipc.mall.mapper.SysOperLogMapper;
import com.ipc.mall.vo.LoginAdmin;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 操作日志切面（PRD §6.7 / docs/api.md A27）。
 * <p>
 * 拦截标注了 {@link OperLog} 的 Controller 方法，记录操作人、模块/动作、目标 id、
 * 请求参数（密码脱敏）、IP、耗时与结果。
 * <p>
 * 三条硬性约束：
 * <ol>
 *     <li><b>不影响主流程</b>：整段记录包在 try-catch 里，写库失败只打 warn；</li>
 *     <li><b>密码不落库</b>：参数名大小写不敏感地命中 password / pwd 时一律写成 {@code ***}；</li>
 *     <li><b>登录失败也要记</b>：登录接口不抛异常，失败由 Controller 通过
 *         {@link OperLogContext#markFailure} 标记，这里读取后写 success=0。</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    private static final int PARAMS_MAX = 2000;
    private static final int ERROR_MAX = 500;
    private static final int NAME_MAX = 20;
    private static final int IP_MAX = 45;
    private static final int URI_MAX = 200;
    private static final String MASK = "***";
    private static final String ANONYMOUS = "匿名";

    /** 参数名命中这些片段就脱敏（大小写不敏感） */
    private static final Set<String> SENSITIVE_KEYS = Set.of("password", "pwd");

    /** JSON 请求体（如商品保存）里的密码字段，序列化后按正则二次兜底 */
    private static final Pattern JSON_SENSITIVE = Pattern.compile(
            "(\"(?:[A-Za-z]*[Pp]assword|pwd)\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE);

    private final SysOperLogMapper sysOperLogMapper;
    private final ObjectMapper objectMapper;

    public OperLogAspect(SysOperLogMapper sysOperLogMapper, ObjectMapper objectMapper) {
        this.sysOperLogMapper = sysOperLogMapper;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        HttpServletRequest request = currentRequest();
        String failure = null;
        try {
            Object result = joinPoint.proceed();
            // 登录这类“失败也不抛异常”的接口，由 Controller 打标记
            failure = OperLogContext.failureOf(request);
            if (failure == null) {
                // 表单校验失败时 Controller 只返回 Result.fail，不会抛异常
                failure = validationFailure(joinPoint);
            }
            return result;
        } catch (Throwable e) {
            failure = e.getMessage() == null || e.getMessage().isBlank()
                    ? e.getClass().getSimpleName()
                    : e.getMessage();
            throw e;
        } finally {
            record(joinPoint, operLog, request, failure, System.currentTimeMillis() - start);
        }
    }

    // ------------------------------------------------------------------ 落库

    private void record(ProceedingJoinPoint joinPoint, OperLog operLog, HttpServletRequest request,
                        String failure, long costMillis) {
        try {
            if (request == null) {
                // 没有 Web 上下文（如直接 new 出来调用）就没有可记录的信息
                return;
            }
            SysOperLog entry = new SysOperLog();
            fillOperator(entry, operLog, request);
            entry.setModule(truncate(operLog.module(), 30));
            entry.setAction(truncate(operLog.action(), 30));
            entry.setTargetId(resolveTargetId(request));
            entry.setRequestUri(truncate(request.getRequestURI(), URI_MAX));
            entry.setRequestMethod(truncate(request.getMethod(), 10));
            entry.setRequestParams(requestParams(joinPoint, request));
            entry.setIp(clientIp(request));
            entry.setSuccess(failure == null ? 1 : 0);
            entry.setErrorMsg(failure == null ? null : truncate(failure, ERROR_MAX));
            entry.setCostMs((int) Math.min(Integer.MAX_VALUE, Math.max(0L, costMillis)));
            entry.setCreateTime(LocalDateTime.now());
            sysOperLogMapper.insert(entry);
            log.debug("操作日志已记录: {} / {} success={} cost={}ms",
                    operLog.module(), operLog.action(), entry.getSuccess(), entry.getCostMs());
        } catch (Exception e) {
            // 审计失败不能拖垮业务
            log.warn("操作日志写入失败，已忽略（模块={} 动作={}）: {}",
                    operLog.module(), operLog.action(), e.toString());
        } finally {
            OperLogContext.clear(request);
        }
    }

    /** 后台操作取会话里的管理员；登录接口还没有会话，取表单用户名并记 admin_id=0 */
    private void fillOperator(SysOperLog entry, OperLog operLog, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(SessionKey.LOGIN_ADMIN);
        if (value instanceof LoginAdmin admin) {
            entry.setAdminId(admin.getAdminId() == null ? 0L : admin.getAdminId());
            entry.setAdminName(truncate(admin.getUsername(), NAME_MAX));
            return;
        }
        entry.setAdminId(0L);
        String username = operLog.login() ? request.getParameter("username") : null;
        String name = username == null || username.isBlank() ? null : truncate(username.trim(), NAME_MAX);
        entry.setAdminName(name == null ? ANONYMOUS : name);
    }

    // ------------------------------------------------------------------ 字段解析

    /** 从方法入参里的 BindingResult 取第一条校验错误；没有错误返回 null */
    private String validationFailure(ProceedingJoinPoint joinPoint) {
        for (Object argument : joinPoint.getArgs()) {
            if (argument instanceof BindingResult bindingResult && bindingResult.hasErrors()) {
                return bindingResult.getAllErrors().isEmpty()
                        ? "参数校验失败"
                        : bindingResult.getAllErrors().get(0).getDefaultMessage();
            }
        }
        return null;
    }

    /** 优先记录表单参数；JSON 请求体（表单参数为空）则退化为序列化业务入参 */
    private String requestParams(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        String formParams = formParamsJson(request);
        if (formParams != null) {
            return formParams;
        }
        for (Object argument : joinPoint.getArgs()) {
            if (!loggable(argument)) {
                continue;
            }
            try {
                String json = objectMapper.writeValueAsString(argument);
                return truncate(JSON_SENSITIVE.matcher(json).replaceAll("$1\"" + MASK + "\""), PARAMS_MAX);
            } catch (JsonProcessingException e) {
                return null;
            }
        }
        return null;
    }

    private String formParamsJson(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return null;
        }
        Map<String, Object> masked = new LinkedHashMap<>();
        parameterMap.forEach((key, values) -> {
            if (isSensitive(key)) {
                masked.put(key, MASK);
            } else if (values == null) {
                masked.put(key, null);
            } else if (values.length == 1) {
                masked.put(key, values[0]);
            } else {
                masked.put(key, Arrays.asList(values));
            }
        });
        try {
            return truncate(objectMapper.writeValueAsString(masked), PARAMS_MAX);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** 目标主键：先看表单 id，再看路径变量（如 /admin/order/{id}/ship） */
    private Long resolveTargetId(HttpServletRequest request) {
        String raw = request.getParameter("id");
        if (raw == null || raw.isBlank()) {
            Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (variables instanceof Map<?, ?> map) {
                Object value = map.get("id");
                raw = value == null ? null : String.valueOf(value);
            }
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip;
        if (forwarded != null && !forwarded.isBlank()) {
            // 可能形如 "client, proxy1, proxy2"，取第一个
            ip = forwarded.split(",")[0].trim();
        } else {
            String realIp = request.getHeader("X-Real-IP");
            ip = realIp != null && !realIp.isBlank() ? realIp.trim() : request.getRemoteAddr();
        }
        return truncate(ip, IP_MAX);
    }

    private boolean loggable(Object argument) {        if (argument == null) {
            return false;
        }
        return !(argument instanceof ServletRequest
                || argument instanceof ServletResponse
                || argument instanceof HttpSession
                || argument instanceof MultipartFile
                || argument instanceof BindingResult);
    }

    private boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.stream().anyMatch(normalized::contains);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
