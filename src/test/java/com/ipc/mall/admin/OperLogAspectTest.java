package com.ipc.mall.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipc.mall.annotation.OperLog;
import com.ipc.mall.aspect.OperLogAspect;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.common.util.OperLogContext;
import com.ipc.mall.dto.AdminResetPasswordForm;
import com.ipc.mall.entity.SysOperLog;
import com.ipc.mall.mapper.SysOperLogMapper;
import com.ipc.mall.vo.LoginAdmin;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 操作日志切面（PRD §6.7）：字段采集、密码脱敏、失败记录、写库异常不影响主流程。
 */
class OperLogAspectTest {

    private static final OperLog RESET_PASSWORD = annotationOf("resetPassword");
    private static final OperLog LOGIN = annotationOf("login");

    private SysOperLogMapper sysOperLogMapper;
    private OperLogAspect aspect;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        sysOperLogMapper = mock(SysOperLogMapper.class);
        aspect = new OperLogAspect(sysOperLogMapper, new ObjectMapper());

        request = new MockHttpServletRequest("POST", "/admin/account/9/reset-password");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ADMIN, new LoginAdmin(7L, "admin", "平台管理员"));
        request.setSession(session);
        request.setRemoteAddr("10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ------------------------------------------------------------ 正常/异常

    @Test
    void 记录成功操作并脱敏密码() throws Throwable {
        request.setParameter("id", "9");
        request.setParameter("password", "abc123");
        request.setParameter("confirmPassword", "abc123");

        Object result = aspect.around(joinPointReturning("ok"), RESET_PASSWORD);

        assertEquals("ok", result);
        SysOperLog log = capturedLog();
        assertEquals("用户管理", log.getModule());
        assertEquals("重置密码", log.getAction());
        assertEquals(7L, log.getAdminId());
        assertEquals("admin", log.getAdminName());
        assertEquals(9L, log.getTargetId());
        assertEquals(1, log.getSuccess());
        assertNull(log.getErrorMsg());
        assertNotNull(log.getCreateTime());
        assertTrue(log.getCostMs() >= 0);
        assertTrue(log.getRequestParams().contains("\"password\":\"***\""), "密码必须脱敏");
        assertFalse(log.getRequestParams().contains("abc123"), "明文密码不能落库");
        assertEquals("10.0.0.8", log.getIp());
    }

    @Test
    void 抛异常时记失败并原样抛出() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenThrow(new BizException(ResultCode.STOCK_NOT_ENOUGH));
        when(joinPoint.getArgs()).thenReturn(new Object[0]);

        BizException exception = assertThrows(BizException.class,
                () -> aspect.around(joinPoint, RESET_PASSWORD));

        assertEquals("库存不足", exception.getMessage());
        SysOperLog log = capturedLog();
        assertEquals(0, log.getSuccess());
        assertEquals("库存不足", log.getErrorMsg());
    }

    // ------------------------------------------------------------ 登录

    @Test
    void 登录失败由Controller标记后记成功为零() throws Throwable {
        MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/admin/login");
        loginRequest.setParameter("username", "hacker");
        loginRequest.setParameter("password", "secret");
        OperLogContext.markFailure(loginRequest, "后台账号或密码错误");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(loginRequest));

        aspect.around(joinPointReturning("admin/login"), LOGIN);

        SysOperLog log = capturedLog();
        assertEquals(0L, log.getAdminId(), "登录时还没有会话，admin_id 记 0");
        assertEquals("hacker", log.getAdminName());
        assertEquals(0, log.getSuccess());
        assertEquals("后台账号或密码错误", log.getErrorMsg());
        assertTrue(log.getRequestParams().contains("\"password\":\"***\""));
    }

    @Test
    void 登录成功记录表单里的用户名() throws Throwable {
        MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/user/login");
        loginRequest.setParameter("username", "xingye");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(loginRequest));

        aspect.around(joinPointReturning("redirect:/"), LOGIN);

        SysOperLog log = capturedLog();
        assertEquals(0L, log.getAdminId());
        assertEquals("xingye", log.getAdminName());
        assertEquals(1, log.getSuccess());
    }

    // ------------------------------------------------------------ 兜底

    @Test
    void 目标id回落到路径变量() throws Throwable {
        Map<String, String> variables = new HashMap<>();
        variables.put("id", "42");
        request.setAttribute(org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                variables);

        aspect.around(joinPointReturning("ok"), RESET_PASSWORD);

        assertEquals(42L, capturedLog().getTargetId());
    }

    @Test
    void JSON请求体也会脱敏() throws Throwable {
        MockHttpServletRequest jsonRequest = new MockHttpServletRequest("POST", "/admin/product/save");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(jsonRequest));

        AdminResetPasswordForm body = new AdminResetPasswordForm();
        body.setPassword("abc123");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("ok");
        when(joinPoint.getArgs()).thenReturn(new Object[]{body});

        aspect.around(joinPoint, RESET_PASSWORD);

        String params = capturedLog().getRequestParams();
        assertNotNull(params);
        assertTrue(params.contains("\"password\":\"***\""));
        assertFalse(params.contains("abc123"));
    }

    @Test
    void 表单校验失败也记失败() throws Throwable {
        // Controller 校验失败只返回 Result.fail，不抛异常；切面要从 BindingResult 识别
        org.springframework.validation.BeanPropertyBindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "form");
        bindingResult.reject("error", "请输入分类名称");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("ok");
        when(joinPoint.getArgs()).thenReturn(new Object[]{bindingResult});

        aspect.around(joinPoint, RESET_PASSWORD);

        SysOperLog log = capturedLog();
        assertEquals(0, log.getSuccess());
        assertEquals("请输入分类名称", log.getErrorMsg());
    }

    @Test
    void 写库失败不影响主流程() throws Throwable {
        when(sysOperLogMapper.insert(any(SysOperLog.class))).thenThrow(new RuntimeException("db down"));

        Object result = aspect.around(joinPointReturning("ok"), RESET_PASSWORD);

        assertEquals("ok", result);
    }

    @Test
    void 没有Web上下文时不记录也不报错() throws Throwable {
        RequestContextHolder.resetRequestAttributes();

        Object result = aspect.around(joinPointReturning("ok"), RESET_PASSWORD);

        assertEquals("ok", result);
        verifyNoInteractions(sysOperLogMapper);
    }

    // ------------------------------------------------------------ 夹具

    private SysOperLog capturedLog() {
        ArgumentCaptor<SysOperLog> captor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(sysOperLogMapper).insert(captor.capture());
        return captor.getValue();
    }

    private ProceedingJoinPoint joinPointReturning(Object value) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn(value);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        return joinPoint;
    }

    private static OperLog annotationOf(String methodName) {
        try {
            return DummyController.class.getDeclaredMethod(methodName).getAnnotation(OperLog.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 只为拿到注解实例，不会被调用 */
    private static class DummyController {

        @OperLog(module = "用户管理", action = "重置密码")
        void resetPassword() {
        }

        @OperLog(module = "认证", action = "后台登录", login = true)
        void login() {
        }
    }
}
