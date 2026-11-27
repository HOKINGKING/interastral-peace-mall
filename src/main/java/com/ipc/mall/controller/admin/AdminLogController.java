package com.ipc.mall.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.common.util.PageUtils;
import com.ipc.mall.service.AdminLogService;
import com.ipc.mall.vo.AdminLogRowVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 后台操作日志（PRD §6.7 / docs/api.md A27）。
 * <p>
 * 只读页面：日志由 {@code @OperLog} + {@code OperLogAspect} 写入，这里只做筛选与分页。
 */
@Controller
public class AdminLogController {

    private static final int PAGE_SIZE = 20;

    private final AdminLogService adminLogService;

    public AdminLogController(AdminLogService adminLogService) {
        this.adminLogService = adminLogService;
    }

    /** A27：按模块 / 操作人 / 时间区间筛选，时间倒序 */
    @GetMapping("/admin/log")
    public String list(@RequestParam(required = false) String module,
                       @RequestParam(required = false) String adminName,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                       @RequestParam(required = false, defaultValue = "1") Integer page,
                       Model model) {
        IPage<AdminLogRowVO> logs = adminLogService.page(module, adminName, dateFrom, dateTo, page, PAGE_SIZE);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("module", module);
        params.put("adminName", adminName);
        params.put("dateFrom", dateFrom);
        params.put("dateTo", dateTo);

        model.addAttribute("pageTitle", "操作日志");
        model.addAttribute("logs", logs.getRecords());
        model.addAttribute("total", logs.getTotal());
        model.addAttribute("pagination", PageUtils.pagination(logs, "/admin/log", params));
        model.addAttribute("modules", adminLogService.modules());
        model.addAttribute("module", module);
        model.addAttribute("adminName", adminName);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        return "admin/log/list";
    }
}
