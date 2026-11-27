package com.ipc.mall.feedback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.FeedbackStatus;
import com.ipc.mall.common.constant.FeedbackType;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.service.CartService;
import com.ipc.mall.service.CategoryService;
import com.ipc.mall.service.FeedbackService;
import com.ipc.mall.vo.FeedbackDetailVO;
import com.ipc.mall.vo.FeedbackFormVO;
import com.ipc.mall.vo.FeedbackRowVO;
import com.ipc.mall.vo.LoginAccount;
import org.junit.jupiter.api.BeforeEach;
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
 * 前台投诉与反馈页面渲染：真实 Thymeleaf + Mock Service，不依赖数据库。
 * <p>
 * 公共头部需要导航分类与购物车角标，这两个依赖也一并 Mock 掉。
 */
@SpringBootTest
@AutoConfigureMockMvc
class FeedbackPageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private CartService cartService;

    @BeforeEach
    void stubLayoutDependencies() {
        when(categoryService.visibleGroups()).thenReturn(List.of());
        when(cartService.count(any())).thenReturn(0);
    }

    @Test
    void 列表页可以渲染() throws Exception {
        Page<FeedbackRowVO> page = new Page<>(1, 8, 1);
        FeedbackRowVO row = new FeedbackRowVO();
        row.setId(1L);
        row.setType(FeedbackType.COMPLAINT);
        row.setTypeDesc("投诉");
        row.setStatus(FeedbackStatus.PENDING.name());
        row.setStatusDesc("待处理");
        row.setStatusClass("badge-pending");
        row.setOrderId(5L);
        row.setOrderNo("20261116000005");
        row.setContentPreview("界面很好看，希望多上架一些新商品");
        row.setReplied(false);
        page.setRecords(List.of(row));
        when(feedbackService.pageMine(any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

        MvcResult result = mockMvc.perform(get("/feedback/list").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/feedback/list"))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("我的投诉与反馈"));
        assertTrue(html.contains("界面很好看"));
        assertTrue(html.contains("20261116000005"));
    }

    @Test
    void 反馈表单可以渲染() throws Exception {
        when(feedbackService.formContext(1L, null)).thenReturn(FeedbackFormVO.feedback());

        MvcResult result = mockMvc.perform(get("/feedback/form").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/feedback/form"))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("意见反馈"));
        assertTrue(html.contains("具体描述"));
        assertTrue(html.contains("商品质量"));
    }

    @Test
    void 投诉详情可以渲染() throws Exception {
        FeedbackDetailVO detail = new FeedbackDetailVO();
        detail.setId(9L);
        detail.setType(FeedbackType.COMPLAINT);
        detail.setTypeDesc("投诉");
        detail.setStatus(FeedbackStatus.REPLIED.name());
        detail.setStatusDesc("已回复");
        detail.setStatusClass("badge-paid");
        detail.setCategory("QUALITY");
        detail.setCategoryDesc("商品质量");
        detail.setContent("商品破损了一角，希望处理一下");
        detail.setCreateTimeText("2026-11-27 13:00:00");
        detail.setReply("已安排补发");
        detail.setReplyTimeText("2026-11-27 14:00:00");
        detail.setCanNote(true);
        detail.setCanResolve(true);
        detail.setNotes(List.of("【2026-11-27 13:30】补充一句说明"));
        FeedbackDetailVO.OrderBrief order = new FeedbackDetailVO.OrderBrief();
        order.setId(5L);
        order.setOrderNo("20261116000005");
        order.setStatusDesc("已完成");
        order.setTotalAmountText("99.00");
        order.setCreateTimeText("2026-11-16 10:21");
        detail.setOrder(order);
        when(feedbackService.detail(1L, 9L)).thenReturn(detail);

        MvcResult result = mockMvc.perform(get("/feedback/9").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/feedback/detail"))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("投诉详情"));
        assertTrue(html.contains("已安排补发"));
        assertTrue(html.contains("补充一句说明"));
        assertTrue(html.contains("20261116000005"));
    }

    private MockHttpSession loginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ACCOUNT, new LoginAccount(1L, "xingye", "星野"));
        session.setAttribute(SessionKey.LOGIN_ACCOUNT_CHECK_AT, System.currentTimeMillis());
        return session;
    }
}
