package com.ipc.mall.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.controller.admin.AdminLogController;
import com.ipc.mall.service.AdminLogService;
import com.ipc.mall.vo.AdminLogRowVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 操作日志接口（A27）：列表渲染与筛选参数透传。
 */
class AdminLogControllerTest {

    private AdminLogService adminLogService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminLogService = mock(AdminLogService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminLogController(adminLogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 日志列表渲染() throws Exception {
        Page<AdminLogRowVO> page = new Page<>(1, 20, 3);
        page.setRecords(List.of(row(1L)));
        when(adminLogService.page(null, null, null, null, 1, 20)).thenReturn(page);
        when(adminLogService.modules()).thenReturn(List.of("订单管理", "认证"));

        mockMvc.perform(get("/admin/log"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/log/list"))
                .andExpect(model().attribute("pageTitle", "操作日志"))
                .andExpect(model().attribute("total", 3L))
                .andExpect(model().attribute("modules", List.of("订单管理", "认证")))
                .andExpect(model().attributeExists("logs", "pagination"));
    }

    @Test
    void 日志列表透传筛选条件() throws Exception {
        when(adminLogService.page(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new Page<>(1, 20, 0));
        when(adminLogService.modules()).thenReturn(List.of());

        mockMvc.perform(get("/admin/log")
                        .param("module", "订单管理")
                        .param("adminName", "admin")
                        .param("dateFrom", "2026-11-01")
                        .param("dateTo", "2026-11-27"))
                .andExpect(status().isOk());

        verify(adminLogService).page(eq("订单管理"), eq("admin"),
                eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 27)), eq(1), eq(20));
    }

    private AdminLogRowVO row(Long id) {
        AdminLogRowVO vo = new AdminLogRowVO();
        vo.setId(id);
        vo.setAdminName("admin");
        vo.setModule("订单管理");
        vo.setAction("发货");
        vo.setSuccess(1);
        vo.setCostMs(12);
        return vo;
    }
}
