package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.entity.SysOperLog;
import com.ipc.mall.mapper.SysOperLogMapper;
import com.ipc.mall.service.AdminLogService;
import com.ipc.mall.vo.AdminLogRowVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

/**
 * 后台操作日志查询实现（PRD §6.7）。
 * <p>
 * 时间区间与订单列表同一口径：结束日期按「次日 00:00」做开区间，含结束当天全天。
 */
@Slf4j
@Service
public class AdminLogServiceImpl implements AdminLogService {

    private final SysOperLogMapper sysOperLogMapper;

    public AdminLogServiceImpl(SysOperLogMapper sysOperLogMapper) {
        this.sysOperLogMapper = sysOperLogMapper;
    }

    @Override
    public IPage<AdminLogRowVO> page(String module, String adminName, LocalDate dateFrom, LocalDate dateTo,
                                     int pageNo, int size) {
        Page<SysOperLog> request = new Page<>(Math.max(1, pageNo), Math.max(1, size));

        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            wrapper.eq(SysOperLog::getModule, module.trim());
        }
        if (StringUtils.hasText(adminName)) {
            wrapper.like(SysOperLog::getAdminName, adminName.trim());
        }
        if (dateFrom != null) {
            wrapper.ge(SysOperLog::getCreateTime, dateFrom.atStartOfDay());
        }
        if (dateTo != null) {
            wrapper.lt(SysOperLog::getCreateTime, dateTo.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(SysOperLog::getCreateTime).orderByDesc(SysOperLog::getId);

        IPage<SysOperLog> raw = sysOperLogMapper.selectPage(request, wrapper);
        Page<AdminLogRowVO> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(raw.getRecords().stream().map(AdminLogRowVO::of).toList());
        return result;
    }

    @Override
    public List<String> modules() {
        List<String> modules = sysOperLogMapper.selectDistinctModules();
        return modules == null ? List.of() : modules;
    }
}
