package com.ipc.mall.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.service.AdminLogService;
import com.ipc.mall.vo.AdminLogRowVO;
import com.ipc.mall.vo.LoginAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 日志页面渲染（A27）：真实 Thymeleaf 引擎 + Mock Service，不依赖数据库。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminLogPageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminLogService adminLogService;

    @Test
    void 日志列表页可以渲染() throws Exception {
        Page<AdminLogRowVO> page = new Page<>(1, 20, 2);
        page.setRecords(List.of(successRow(), failedRow()));
        when(adminLogService.page(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(page);
        when(adminLogService.modules()).thenReturn(List.of("订单管理", "认证"));

        MvcResult result = mockMvc.perform(get("/admin/log").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/log/list"))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("操作日志"), "页面缺少标题");
        assertTrue(html.contains("订单管理"), "列表缺少模块名");
        assertTrue(html.contains("发货"), "列表缺少动作");
        assertTrue(html.contains("***"), "参数里的密码应脱敏展示");
        assertTrue(html.contains("成功"), "缺少成功徽标");
        assertTrue(html.contains("用户不存在或已被删除"), "失败行应展示错误原因");
        assertTrue(html.contains("10.0.0.8"), "缺少 IP 列");
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ADMIN, new LoginAdmin(1L, "admin", "平台管理员"));
        session.setAttribute(SessionKey.LOGIN_ADMIN_CHECK_AT, System.currentTimeMillis());
        return session;
    }

    private AdminLogRowVO successRow() {
        AdminLogRowVO row = new AdminLogRowVO();
        row.setId(1L);
        row.setAdminId(1L);
        row.setAdminName("admin");
        row.setModule("订单管理");
        row.setAction("发货");
        row.setTargetId(12L);
        row.setRequestParams("{\"courierCompany\":\"星际速运\",\"password\":\"***\"}");
        row.setIp("10.0.0.8");
        row.setSuccess(1);
        row.setCostMs(12);
        return row;
    }

    private AdminLogRowVO failedRow() {
        AdminLogRowVO row = new AdminLogRowVO();
        row.setId(2L);
        row.setAdminName("admin");
        row.setModule("用户管理");
        row.setAction("重置密码");
        row.setSuccess(0);
        row.setErrorMsg("用户不存在或已被删除");
        row.setCostMs(3);
        return row;
    }
}
