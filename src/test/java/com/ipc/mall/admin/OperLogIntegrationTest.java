package com.ipc.mall.admin;

import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.controller.admin.AdminCategoryController;
import com.ipc.mall.controller.admin.AdminLogController;
import com.ipc.mall.controller.admin.AdminLoginController;
import com.ipc.mall.controller.admin.AdminOrderController;
import com.ipc.mall.entity.SysOperLog;
import com.ipc.mall.mapper.SysOperLogMapper;
import com.ipc.mall.service.CategoryAdminService;
import com.ipc.mall.vo.LoginAdmin;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 操作日志的 Spring AOP 装配（PRD §6.7）：注解的写操作会被切面代理并落库，
 * 登录失败也能记录；没有注解的只读接口不加代理。用 MockBean 顶掉业务与 Mapper，不依赖数据库。
 */
@SpringBootTest
@AutoConfigureMockMvc
class OperLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private CategoryAdminService categoryAdminService;

    @MockBean
    private SysOperLogMapper sysOperLogMapper;

    @Test
    void 保存分类会写操作日志() throws Exception {
        mockMvc.perform(post("/admin/category/save")
                        .session(adminSession())
                        .param("name", "测试分类"))
                .andExpect(status().isOk());

        ArgumentCaptor<SysOperLog> captor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(sysOperLogMapper).insert(captor.capture());
        SysOperLog log = captor.getValue();
        assertEquals("分类管理", log.getModule());
        assertEquals("保存分类", log.getAction());
        assertEquals(7L, log.getAdminId());
        assertEquals("admin", log.getAdminName());
        assertEquals("POST", log.getRequestMethod());
        assertEquals("/admin/category/save", log.getRequestUri());
        assertEquals(1, log.getSuccess());
        assertTrue(log.getRequestParams().contains("测试分类"));
    }

    @Test
    void 后台登录失败也会记录() throws Exception {
        // 新会话没有验证码，必然校验失败；登录接口不抛异常，靠 OperLogContext 标记失败
        mockMvc.perform(post("/admin/login")
                        .session(new MockHttpSession())
                        .param("username", "hacker")
                        .param("password", "secret")
                        .param("captcha", "ABCD"))
                .andExpect(status().isOk());

        ArgumentCaptor<SysOperLog> captor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(sysOperLogMapper).insert(captor.capture());
        SysOperLog log = captor.getValue();
        assertEquals("认证", log.getModule());
        assertEquals("后台登录", log.getAction());
        assertEquals(0L, log.getAdminId(), "登录失败时 admin_id 记 0");
        assertEquals("hacker", log.getAdminName());
        assertEquals(0, log.getSuccess());
        assertEquals("验证码错误或已过期", log.getErrorMsg());
        assertFalse(log.getRequestParams().contains("secret"), "密码不能明文落库");
    }

    @Test
    void 只给带注解的写操作加代理() {
        assertTrue(AopUtils.isAopProxy(applicationContext.getBean(AdminCategoryController.class)));
        assertTrue(AopUtils.isAopProxy(applicationContext.getBean(AdminOrderController.class)));
        assertTrue(AopUtils.isAopProxy(applicationContext.getBean(AdminLoginController.class)));
        // 只读的日志页没有 @OperLog，不需要代理
        assertFalse(AopUtils.isAopProxy(applicationContext.getBean(AdminLogController.class)));
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ADMIN, new LoginAdmin(7L, "admin", "平台管理员"));
        session.setAttribute(SessionKey.LOGIN_ADMIN_CHECK_AT, System.currentTimeMillis());
        return session;
    }
}
