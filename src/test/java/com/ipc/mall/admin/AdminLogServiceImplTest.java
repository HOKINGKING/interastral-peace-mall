package com.ipc.mall.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.entity.SysOperLog;
import com.ipc.mall.mapper.SysOperLogMapper;
import com.ipc.mall.service.impl.AdminLogServiceImpl;
import com.ipc.mall.vo.AdminLogRowVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 操作日志查询（PRD §6.7 / A27）：行映射、结果/耗时文案、模块选项兜底。
 */
class AdminLogServiceImplTest {

    private SysOperLogMapper sysOperLogMapper;
    private AdminLogServiceImpl service;

    @BeforeEach
    void setUp() {
        sysOperLogMapper = mock(SysOperLogMapper.class);
        service = new AdminLogServiceImpl(sysOperLogMapper);
    }

    @Test
    void 列表把日志映射成行() {
        Page<SysOperLog> raw = new Page<>(1, 20, 1);
        SysOperLog log = new SysOperLog();
        log.setId(1L);
        log.setAdminId(7L);
        log.setAdminName("admin");
        log.setModule("订单管理");
        log.setAction("发货");
        log.setTargetId(12L);
        log.setRequestParams("{\"courierCompany\":\"星际速运\"}");
        log.setIp("10.0.0.8");
        log.setSuccess(1);
        log.setCostMs(12);
        log.setCreateTime(LocalDateTime.of(2026, 11, 27, 13, 0, 0));
        raw.setRecords(List.of(log));
        doReturn(raw).when(sysOperLogMapper).selectPage(any(), any());

        IPage<AdminLogRowVO> page = service.page("订单管理", "admin",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 27), 1, 20);

        assertEquals(1, page.getTotal());
        AdminLogRowVO row = page.getRecords().get(0);
        assertEquals("订单管理", row.getModule());
        assertEquals("发货", row.getAction());
        assertEquals("#12", row.getTargetText());
        assertEquals("12 ms", row.getCostText());
        assertEquals("成功", row.getResultDesc());
        assertEquals("badge-completed", row.getResultClass());
        assertEquals("2026-11-27 13:00:00", row.getCreateTimeText());
        assertTrue(row.getParamsPreview().contains("星际速运"));
    }

    @Test
    void 失败日志展示红色结果与错误原因() {
        Page<SysOperLog> raw = new Page<>(1, 20, 1);
        SysOperLog log = new SysOperLog();
        log.setId(2L);
        log.setAdminName("admin");
        log.setModule("用户管理");
        log.setAction("重置密码");
        log.setSuccess(0);
        log.setErrorMsg("用户不存在或已被删除");
        log.setCostMs(3);
        raw.setRecords(List.of(log));
        doReturn(raw).when(sysOperLogMapper).selectPage(any(), any());

        AdminLogRowVO row = service.page(null, null, null, null, 1, 20).getRecords().get(0);

        assertEquals("失败", row.getResultDesc());
        assertEquals("badge-danger", row.getResultClass());
        assertTrue(row.isFailed());
        assertEquals("用户不存在或已被删除", row.getErrorMsg());
        assertEquals("—", row.getTargetText());
        assertEquals("—", row.getParamsPreview());
    }

    @Test
    void 参数预览超长时截断() {
        Page<SysOperLog> raw = new Page<>(1, 20, 1);
        SysOperLog log = new SysOperLog();
        log.setRequestParams("x".repeat(200));
        raw.setRecords(List.of(log));
        doReturn(raw).when(sysOperLogMapper).selectPage(any(), any());

        AdminLogRowVO row = service.page(null, null, null, null, 1, 20).getRecords().get(0);

        assertEquals(81, row.getParamsPreview().length(), "80 个字符 + 省略号");
        assertTrue(row.getParamsPreview().endsWith("…"));
    }

    @Test
    void 模块列表为空时返回空集合() {
        when(sysOperLogMapper.selectDistinctModules()).thenReturn(null);

        assertTrue(service.modules().isEmpty());
    }
}
