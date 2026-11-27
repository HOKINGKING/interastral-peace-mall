package com.ipc.mall.feedback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.FeedbackStatus;
import com.ipc.mall.common.constant.FeedbackType;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.front.FeedbackController;
import com.ipc.mall.dto.FeedbackForm;
import com.ipc.mall.service.FeedbackService;
import com.ipc.mall.vo.FeedbackDetailVO;
import com.ipc.mall.vo.FeedbackFormVO;
import com.ipc.mall.vo.FeedbackRowVO;
import com.ipc.mall.vo.LoginAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 前台投诉与反馈接口（F32–F39）：页面跳转、表单校验、PRG 与越权回跳。
 */
class FeedbackControllerTest {

    private FeedbackService feedbackService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        feedbackService = mock(FeedbackService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new FeedbackController(feedbackService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ------------------------------------------------------------ 列表 / 表单

    @Test
    void 列表渲染() throws Exception {
        Page<FeedbackRowVO> page = new Page<>(1, 8, 2);
        page.setRecords(List.of(row(1L)));
        when(feedbackService.pageMine(eq(1L), eq(null), eq(null), eq(1), eq(8))).thenReturn(page);

        mockMvc.perform(get("/feedback/list").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/feedback/list"))
                .andExpect(model().attribute("total", 2L))
                .andExpect(model().attributeExists("feedbacks", "pagination", "typeTabs"));
    }

    @Test
    void 反馈表单渲染() throws Exception {
        when(feedbackService.formContext(1L, null)).thenReturn(FeedbackFormVO.feedback());

        mockMvc.perform(get("/feedback/form").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/feedback/form"))
                .andExpect(model().attribute("pageTitle", "意见反馈"))
                .andExpect(model().attributeExists("form", "feedbackForm"));
    }

    @Test
    void 投诉表单带订单快照() throws Exception {
        FeedbackFormVO context = new FeedbackFormVO();
        context.setComplaint(true);
        context.setOrderId(5L);
        context.setOrderNo("20261116000005");
        context.setTotalAmountText("99.00");
        context.setOrderStatusDesc("已支付");
        when(feedbackService.formContext(1L, 5L)).thenReturn(context);

        mockMvc.perform(get("/feedback/form").param("orderId", "5").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pageTitle", "订单投诉"));
    }

    @Test
    void 非法订单的投诉表单回跳列表() throws Exception {
        when(feedbackService.formContext(1L, 5L))
                .thenThrow(new BizException(ResultCode.FEEDBACK_ORDER_INVALID));

        mockMvc.perform(get("/feedback/form").param("orderId", "5").session(loginSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feedback/list"))
                .andExpect(flash().attribute("listMessage", ResultCode.FEEDBACK_ORDER_INVALID.getMessage()));
    }

    // ------------------------------------------------------------ 提交

    @Test
    void 提交成功重定向到我的反馈() throws Exception {
        mockMvc.perform(post("/feedback/submit")
                        .session(loginSession())
                        .param("content", "界面很好看，希望多上架一些新商品"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feedback/list"))
                .andExpect(flash().attribute("listMessage", "已提交，卖家会尽快处理"));

        verify(feedbackService).submit(eq(1L), any(FeedbackForm.class));
    }

    @Test
    void 内容过短重新渲染表单() throws Exception {
        when(feedbackService.formContext(1L, null)).thenReturn(FeedbackFormVO.feedback());

        mockMvc.perform(post("/feedback/submit")
                        .session(loginSession())
                        .param("content", "短"))
                .andExpect(status().isOk())
                .andExpect(view().name("front/feedback/form"))
                .andExpect(model().attributeExists("formError"));

        verify(feedbackService, never()).submit(any(), any());
    }

    @Test
    void 业务失败重新渲染表单并保留提示() throws Exception {
        when(feedbackService.formContext(1L, null)).thenReturn(FeedbackFormVO.feedback());
        org.mockito.Mockito.doThrow(new BizException(ResultCode.FEEDBACK_RATE_LIMIT))
                .when(feedbackService).submit(eq(1L), any(FeedbackForm.class));

        mockMvc.perform(post("/feedback/submit")
                        .session(loginSession())
                        .param("content", "界面很好看，希望多上架一些新商品"))
                .andExpect(status().isOk())
                .andExpect(view().name("front/feedback/form"))
                .andExpect(model().attribute("formError", ResultCode.FEEDBACK_RATE_LIMIT.getMessage()));
    }

    // ------------------------------------------------------------ 详情 / 流转

    @Test
    void 详情渲染() throws Exception {
        FeedbackDetailVO detail = new FeedbackDetailVO();
        detail.setId(9L);
        detail.setType(FeedbackType.COMPLAINT);
        detail.setTypeDesc("投诉");
        detail.setStatus(FeedbackStatus.REPLIED.name());
        detail.setStatusDesc("已回复");
        detail.setStatusClass("badge-paid");
        detail.setCanNote(true);
        detail.setCanResolve(true);
        when(feedbackService.detail(1L, 9L)).thenReturn(detail);

        mockMvc.perform(get("/feedback/9").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/feedback/detail"))
                .andExpect(model().attribute("pageTitle", "投诉详情"))
                .andExpect(model().attribute("feedback", detail));
    }

    @Test
    void 他人反馈回跳列表() throws Exception {
        when(feedbackService.detail(1L, 9L))
                .thenThrow(new BizException(ResultCode.NOT_FOUND.getCode(), "投诉或反馈不存在"));

        mockMvc.perform(get("/feedback/9").session(loginSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feedback/list"))
                .andExpect(flash().attribute("listMessage", "投诉或反馈不存在"));
    }

    @Test
    void 撤销后回到详情() throws Exception {
        mockMvc.perform(post("/feedback/9/cancel").session(loginSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feedback/9"))
                .andExpect(flash().attribute("listMessage", "已撤销，卖家将不再处理"));

        verify(feedbackService).cancel(1L, 9L);
    }

    @Test
    void 补充说明成功后回到详情() throws Exception {
        mockMvc.perform(post("/feedback/9/note")
                        .session(loginSession())
                        .param("note", "补充一句说明"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feedback/9"));

        verify(feedbackService).addNote(1L, 9L, "补充一句说明");
    }

    @Test
    void 空补充说明提示后回到详情() throws Exception {
        mockMvc.perform(post("/feedback/9/note")
                        .session(loginSession())
                        .param("note", " "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feedback/9"))
                .andExpect(flash().attributeExists("listMessage"));

        verify(feedbackService, never()).addNote(any(), any(), any());
    }

    @Test
    void 确认解决后回到详情() throws Exception {
        mockMvc.perform(post("/feedback/9/resolve").session(loginSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feedback/9"));

        verify(feedbackService).resolve(1L, 9L);
    }

    // ------------------------------------------------------------ 夹具

    private MockHttpSession loginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ACCOUNT, new LoginAccount(1L, "xingye", "星野"));
        return session;
    }

    private FeedbackRowVO row(Long id) {
        FeedbackRowVO vo = new FeedbackRowVO();
        vo.setId(id);
        vo.setType(FeedbackType.FEEDBACK);
        vo.setTypeDesc("反馈");
        vo.setStatus(FeedbackStatus.PENDING.name());
        vo.setStatusDesc("待处理");
        vo.setStatusClass("badge-pending");
        vo.setContentPreview("界面很好看");
        vo.setReplied(false);
        return vo;
    }
}
