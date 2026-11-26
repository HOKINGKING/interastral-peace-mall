package com.ipc.mall.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.admin.AdminOrderController;
import com.ipc.mall.dto.ReceiverForm;
import com.ipc.mall.dto.ShipForm;
import com.ipc.mall.service.AdminOrderService;
import com.ipc.mall.vo.AdminOrderRowVO;
import com.ipc.mall.vo.OrderDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 后台订单管理接口（A22–A26）。
 */
class AdminOrderControllerTest {

    private AdminOrderService adminOrderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminOrderService = mock(AdminOrderService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOrderController(adminOrderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ------------------------------------------------------------ A22 列表

    @Test
    void 订单列表渲染() throws Exception {
        Page<AdminOrderRowVO> page = new Page<>(1, 10, 5);
        page.setRecords(List.of(row(1L, "20261117201501")));
        when(adminOrderService.page(null, null, null, null, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/admin/order"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/order/list"))
                .andExpect(model().attribute("pageTitle", "订单管理"))
                .andExpect(model().attribute("total", 5L))
                .andExpect(model().attributeExists("orders", "pagination", "statusOptions"));
    }

    @Test
    void 订单列表按条件筛选() throws Exception {
        when(adminOrderService.page(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new Page<>(1, 10, 0));

        mockMvc.perform(get("/admin/order")
                        .param("orderNo", "202611")
                        .param("username", "xingye")
                        .param("status", "PAID")
                        .param("dateFrom", "2026-11-01")
                        .param("dateTo", "2026-11-25"))
                .andExpect(status().isOk());

        verify(adminOrderService).page(eq("202611"), eq("xingye"), eq("PAID"),
                eq(java.time.LocalDate.of(2026, 11, 1)), eq(java.time.LocalDate.of(2026, 11, 25)), eq(1), eq(10));
    }

    // ------------------------------------------------------------ A23 详情

    @Test
    void 订单详情渲染() throws Exception {
        OrderDetailVO detail = new OrderDetailVO();
        detail.setId(2L);
        detail.setOrderNo("20261116102133");
        detail.setUsername("xingye");
        detail.setStatusDesc("已完成");
        detail.setStatusClass("badge-completed");
        when(adminOrderService.detail(2L)).thenReturn(detail);

        mockMvc.perform(get("/admin/order/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/order/detail"))
                .andExpect(model().attribute("pageTitle", "订单详情"))
                .andExpect(model().attribute("order", detail));
    }

    @Test
    void 订单不存在回列表并提示() throws Exception {
        when(adminOrderService.detail(9L)).thenThrow(new BizException(ResultCode.ORDER_NOT_FOUND));

        mockMvc.perform(get("/admin/order/9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/order"))
                .andExpect(flash().attribute("listMessage", ResultCode.ORDER_NOT_FOUND.getMessage()));
    }

    // ------------------------------------------------------------ A24 发货

    @Test
    void 发货成功() throws Exception {
        mockMvc.perform(post("/admin/order/5/ship")
                        .param("courierCompany", "星际速运")
                        .param("trackingNo", "IPC202611170002"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已发货，订单状态更新为已发货"));

        verify(adminOrderService).ship(eq(5L), any(ShipForm.class));
    }

    @Test
    void 发货缺少物流公司返回四百() throws Exception {
        mockMvc.perform(post("/admin/order/5/ship")
                        .param("courierCompany", " ")
                        .param("trackingNo", "IPC202611170002"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请输入物流公司"));

        verify(adminOrderService, never()).ship(any(), any());
    }

    @Test
    void 重复发货返回三千零二() throws Exception {
        doThrow(new BizException(ResultCode.ORDER_STATUS_INVALID)).when(adminOrderService).ship(eq(5L), any(ShipForm.class));

        mockMvc.perform(post("/admin/order/5/ship")
                        .param("courierCompany", "星际速运")
                        .param("trackingNo", "IPC202611170002"))
                .andExpect(jsonPath("$.code").value(3002));
    }

    // ------------------------------------------------------------ A25 改收货信息

    @Test
    void 修改收货信息成功() throws Exception {
        mockMvc.perform(post("/admin/order/5/receiver")
                        .param("receiverName", "陈开拓")
                        .param("receiverPhone", "13800000002")
                        .param("receiverDetail", "航道街 10 号")
                        .param("remark", "放前台"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("收货信息已更新"));

        verify(adminOrderService).updateReceiver(eq(5L), any(ReceiverForm.class));
    }

    @Test
    void 修改收货信息手机号非法返回四百() throws Exception {
        mockMvc.perform(post("/admin/order/5/receiver")
                        .param("receiverName", "陈开拓")
                        .param("receiverPhone", "123")
                        .param("receiverDetail", "航道街 10 号"))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("手机号需为 11 位数字"));

        verify(adminOrderService, never()).updateReceiver(any(), any());
    }

    // ------------------------------------------------------------ A26 删除

    @Test
    void 删除订单成功() throws Exception {
        mockMvc.perform(delete("/admin/order/3"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("订单已删除"));

        verify(adminOrderService).remove(3L);
    }

    @Test
    void 删除非终态订单返回三千零二() throws Exception {
        doThrow(new BizException(ResultCode.ORDER_STATUS_INVALID)).when(adminOrderService).remove(5L);

        mockMvc.perform(delete("/admin/order/5"))
                .andExpect(jsonPath("$.code").value(3002));
    }

    // ------------------------------------------------------------ 夹具

    private AdminOrderRowVO row(Long id, String orderNo) {
        AdminOrderRowVO vo = new AdminOrderRowVO();
        vo.setId(id);
        vo.setOrderNo(orderNo);
        vo.setUsername("xingye");
        vo.setNickname("星野");
        vo.setTotalAmount(new BigDecimal("148.00"));
        vo.setStatus("PENDING_PAYMENT");
        vo.setItemCount(2);
        return vo;
    }
}
