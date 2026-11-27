package com.ipc.mall.feedback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.FeedbackStatus;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.admin.AdminFeedbackController;
import com.ipc.mall.dto.FeedbackCloseForm;
import com.ipc.mall.dto.FeedbackReplyForm;
import com.ipc.mall.service.AdminFeedbackService;
import com.ipc.mall.vo.AdminFeedbackRowVO;
import com.ipc.mall.vo.FeedbackDetailVO;
import com.ipc.mall.vo.LoginAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 后台投诉与反馈接口（A28–A32）：筛选、详情回跳、受理/回复/关闭的校验。
 */
class AdminFeedbackControllerTest {

    private AdminFeedbackService adminFeedbackService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminFeedbackService = mock(AdminFeedbackService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminFeedbackController(adminFeedbackService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ------------------------------------------------------------ A28 列表

    @Test
    void 列表渲染() throws Exception {
        Page<AdminFeedbackRowVO> page = new Page<>(1, 10, 3);
        page.setRecords(List.of(row(1L)));
        when(adminFeedbackService.page(null, null, null, null, null, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/admin/feedback"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/feedback/list"))
                .andExpect(model().attribute("total", 3L))
                .andExpect(model().attributeExists("feedbacks", "pagination"));
    }

    @Test
    void 列表透传筛选条件() throws Exception {
        when(adminFeedbackService.page(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new Page<>());

        mockMvc.perform(get("/admin/feedback")
                        .param("type", "COMPLAINT")
                        .param("status", "PENDING")
                        .param("orderNo", "202611")
                        .param("username", "xingye")
                        .param("dateFrom", "2026-11-01")
                        .param("dateTo", "2026-11-27"))
                .andExpect(status().isOk());

        verify(adminFeedbackService).page(eq("COMPLAINT"), eq("PENDING"), eq("202611"), eq("xingye"),
                eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 27)), eq(1), eq(10));
    }

    // ------------------------------------------------------------ A29 详情

    @Test
    void 详情渲染() throws Exception {
        FeedbackDetailVO detail = new FeedbackDetailVO();
        detail.setId(9L);
        detail.setTypeDesc("投诉");
        detail.setStatusDesc("待处理");
        detail.setStatusClass("badge-pending");
        detail.setCanAccept(true);
        detail.setCanReply(true);
        detail.setCanClose(true);
        when(adminFeedbackService.detail(9L)).thenReturn(detail);

        mockMvc.perform(get("/admin/feedback/9"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/feedback/detail"))
                .andExpect(model().attribute("feedback", detail));
    }

    @Test
    void 详情不存在回列表() throws Exception {
        when(adminFeedbackService.detail(9L))
                .thenThrow(new BizException(ResultCode.NOT_FOUND.getCode(), "投诉或反馈不存在或已被删除"));

        mockMvc.perform(get("/admin/feedback/9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/feedback"))
                .andExpect(flash().attribute("listMessage", "投诉或反馈不存在或已被删除"));
    }

    // ------------------------------------------------------------ A30 受理

    @Test
    void 受理成功() throws Exception {
        mockMvc.perform(post("/admin/feedback/9/accept"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已受理，状态更新为处理中"));

        verify(adminFeedbackService).accept(9L);
    }

    @Test
    void 重复受理返回三千零二() throws Exception {
        org.mockito.Mockito.doThrow(new BizException(ResultCode.ORDER_STATUS_INVALID))
                .when(adminFeedbackService).accept(9L);

        mockMvc.perform(post("/admin/feedback/9/accept"))
                .andExpect(jsonPath("$.code").value(3002));
    }

    // ------------------------------------------------------------ A31 回复

    @Test
    void 回复成功带管理员id() throws Exception {
        mockMvc.perform(post("/admin/feedback/9/reply")
                        .session(adminSession())
                        .param("reply", "已安排补发"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已回复，等待用户确认"));

        verify(adminFeedbackService).reply(9L, "已安排补发", 7L);
    }

    @Test
    void 空回复返回四百() throws Exception {
        mockMvc.perform(post("/admin/feedback/9/reply")
                        .session(adminSession())
                        .param("reply", " "))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请填写回复内容"));

        verify(adminFeedbackService, never()).reply(any(), any(), any());
    }

    // ------------------------------------------------------------ A32 关闭

    @Test
    void 关闭成功() throws Exception {
        mockMvc.perform(post("/admin/feedback/9/close")
                        .param("closeReason", "已协商解决"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已关闭"));

        verify(adminFeedbackService).close(9L, "已协商解决");
    }

    @Test
    void 空关闭原因返回四百() throws Exception {
        mockMvc.perform(post("/admin/feedback/9/close")
                        .param("closeReason", ""))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请填写关闭原因"));

        verify(adminFeedbackService, never()).close(any(), any());
    }

    // ------------------------------------------------------------ 夹具

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ADMIN, new LoginAdmin(7L, "admin", "平台管理员"));
        return session;
    }

    private AdminFeedbackRowVO row(Long id) {
        AdminFeedbackRowVO vo = new AdminFeedbackRowVO();
        vo.setId(id);
        vo.setType("COMPLAINT");
        vo.setStatus(FeedbackStatus.PENDING.name());
        vo.setContent("商品破损");
        vo.setUsername("xingye");
        vo.setNickname("星野");
        vo.setOrderNo("20261116000005");
        return vo;
    }
}
