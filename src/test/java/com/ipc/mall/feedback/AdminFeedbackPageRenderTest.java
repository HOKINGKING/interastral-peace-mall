package com.ipc.mall.feedback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.FeedbackStatus;
import com.ipc.mall.common.constant.FeedbackType;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.service.AdminFeedbackService;
import com.ipc.mall.vo.AdminFeedbackRowVO;
import com.ipc.mall.vo.FeedbackDetailVO;
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
 * 后台投诉与反馈页面渲染：真实 Thymeleaf + Mock Service，不依赖数据库。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminFeedbackPageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminFeedbackService adminFeedbackService;

    @Test
    void 列表页可以渲染() throws Exception {
        Page<AdminFeedbackRowVO> page = new Page<>(1, 10, 1);
        AdminFeedbackRowVO row = new AdminFeedbackRowVO();
        row.setId(1L);
        row.setType(FeedbackType.COMPLAINT);
        row.setStatus(FeedbackStatus.PENDING.name());
        row.setUsername("xingye");
        row.setNickname("星野");
        row.setOrderId(5L);
        row.setOrderNo("20261116000005");
        row.setContent("商品破损了一角，希望处理一下");
        row.setReplied(false);
        page.setRecords(List.of(row));
        when(adminFeedbackService.page(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page);

        MvcResult result = mockMvc.perform(get("/admin/feedback").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/feedback/list"))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("投诉与反馈"));
        assertTrue(html.contains("商品破损了一角"));
        assertTrue(html.contains("20261116000005"));
    }

    @Test
    void 详情页可以渲染() throws Exception {
        FeedbackDetailVO detail = new FeedbackDetailVO();
        detail.setId(9L);
        detail.setType(FeedbackType.COMPLAINT);
        detail.setTypeDesc("投诉");
        detail.setStatus(FeedbackStatus.PENDING.name());
        detail.setStatusDesc("待处理");
        detail.setStatusClass("badge-pending");
        detail.setCategory("QUALITY");
        detail.setCategoryDesc("商品质量");
        detail.setContent("商品破损了一角，希望处理一下");
        detail.setCreateTimeText("2026-11-27 13:00:00");
        detail.setNotes(List.of("【2026-11-27 13:30】补充一句说明"));
        detail.setUsername("xingye");
        detail.setNickname("星野");
        detail.setCanAccept(true);
        detail.setCanReply(true);
        detail.setCanClose(true);
        FeedbackDetailVO.OrderBrief order = new FeedbackDetailVO.OrderBrief();
        order.setId(5L);
        order.setOrderNo("20261116000005");
        order.setStatusDesc("已完成");
        order.setTotalAmountText("99.00");
        order.setCreateTimeText("2026-11-16 10:21");
        detail.setOrder(order);
        when(adminFeedbackService.detail(9L)).thenReturn(detail);

        MvcResult result = mockMvc.perform(get("/admin/feedback/9").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/feedback/detail"))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("问题描述"));
        assertTrue(html.contains("用户补充说明"));
        assertTrue(html.contains("补充一句说明"));
        assertTrue(html.contains("受理"));
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ADMIN, new LoginAdmin(7L, "admin", "平台管理员"));
        session.setAttribute(SessionKey.LOGIN_ADMIN_CHECK_AT, System.currentTimeMillis());
        return session;
    }
}
