package com.ipc.mall.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.service.AccountAdminService;
import com.ipc.mall.vo.AdminAccountDetailVO;
import com.ipc.mall.vo.AdminAccountRowVO;
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
 * 用户管理页面渲染（A18/A19）：用真实 Thymeleaf 引擎渲染模板，Service 用 Mock 顶掉，不依赖数据库。
 * <p>
 * 单元测试只能保证 Controller 返回值，模板里的表达式/片段引用错误只有真正渲染才会暴露，
 * 所以这里补一层“模板不炸”的保障。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminAccountPageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountAdminService accountAdminService;

    @Test
    void 用户列表页可以渲染() throws Exception {
        Page<AdminAccountRowVO> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(row(1L, "xingye"), row(2L, "bob")));
        when(accountAdminService.page(any(), any(), anyInt(), anyInt())).thenReturn(page);

        MvcResult result = mockMvc.perform(get("/admin/account").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/account/list"))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("用户管理"), "列表页缺少标题");
        assertTrue(html.contains("xingye"), "列表页没有渲染用户名");
        assertTrue(html.contains("138****0001"), "手机号没有打码");
        assertTrue(html.contains("用户管理"), "侧边栏缺少用户管理入口");
    }

    @Test
    void 用户详情页可以渲染() throws Exception {
        AdminAccountDetailVO detail = new AdminAccountDetailVO();
        detail.setId(1L);
        detail.setUsername("xingye");
        detail.setNickname("星野");
        detail.setPhone("13800000001");
        detail.setPhoneMasked("138****0001");
        detail.setStatus(1);
        detail.setOrderCount(3L);
        when(accountAdminService.detail(1L)).thenReturn(detail);

        MvcResult result = mockMvc.perform(get("/admin/account/1").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/account/detail"))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("基本资料"), "详情页缺少基本资料表单");
        assertTrue(html.contains("13800000001"), "详情页应回填完整手机号");
        assertTrue(html.contains("重置密码"), "详情页缺少重置密码表单");
        assertTrue(html.contains(">3<"), "详情页没有渲染订单数");
    }

    /** 已登录且状态回查时间戳是新的：拦截器不会再查库 */
    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ADMIN, new LoginAdmin(1L, "admin", "平台管理员"));
        session.setAttribute(SessionKey.LOGIN_ADMIN_CHECK_AT, System.currentTimeMillis());
        return session;
    }

    private AdminAccountRowVO row(Long id, String username) {
        AdminAccountRowVO vo = new AdminAccountRowVO();
        vo.setId(id);
        vo.setUsername(username);
        vo.setNickname("星野");
        vo.setPhoneMasked("138****0001");
        vo.setStatus(1);
        return vo;
    }
}
