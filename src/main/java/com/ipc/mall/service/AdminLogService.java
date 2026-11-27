package com.ipc.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.vo.AdminLogRowVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 后台操作日志查询（PRD §6.7 / docs/api.md A27）。
 * <p>
 * 只读：日志由 {@code OperLogAspect} 写入，后台只能查、不能改（审计要求）。
 */
public interface AdminLogService {

    /** A27：分页查询，支持模块 / 操作人 / 时间区间筛选，按时间倒序 */
    IPage<AdminLogRowVO> page(String module, String adminName, LocalDate dateFrom, LocalDate dateTo,
                              int pageNo, int size);

    /** 模块下拉选项（库里出现过的模块名） */
    List<String> modules();
}
