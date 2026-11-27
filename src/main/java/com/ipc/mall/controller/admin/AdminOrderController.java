package com.ipc.mall.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.annotation.OperLog;
import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.common.util.PageUtils;
import com.ipc.mall.dto.ReceiverForm;
import com.ipc.mall.dto.ShipForm;
import com.ipc.mall.service.AdminOrderService;
import com.ipc.mall.vo.AdminOrderRowVO;
import com.ipc.mall.vo.OrderDetailVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台订单管理（PRD §6.5 / docs/api.md A22–A26）。
 * <p>
 * 列表/详情返回视图；发货、改收货信息、删除返回 {@code Result}（页面用 fetch 提交）。
 */
@Controller
public class AdminOrderController {

    private static final int PAGE_SIZE = 10;

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    /** A22：订单列表（订单号 / 用户名 / 状态 / 下单时间区间） */
    @GetMapping("/admin/order")
    public String list(@RequestParam(required = false) String orderNo,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                       @RequestParam(required = false, defaultValue = "1") Integer page,
                       Model model) {
        IPage<AdminOrderRowVO> orders = adminOrderService.page(orderNo, username, status,
                dateFrom, dateTo, page, PAGE_SIZE);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("orderNo", orderNo);
        params.put("username", username);
        params.put("status", status);
        params.put("dateFrom", dateFrom);
        params.put("dateTo", dateTo);

        model.addAttribute("pageTitle", "订单管理");
        model.addAttribute("orders", orders.getRecords());
        model.addAttribute("total", orders.getTotal());
        model.addAttribute("pagination", PageUtils.pagination(orders, "/admin/order", params));
        model.addAttribute("statusOptions", statusOptions());
        model.addAttribute("orderNo", orderNo);
        model.addAttribute("username", username);
        model.addAttribute("status", status);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        return "admin/order/list";
    }

    /** A23：订单详情（含发货与改收货信息入口） */
    @GetMapping("/admin/order/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            OrderDetailVO order = adminOrderService.detail(id);
            model.addAttribute("pageTitle", "订单详情");
            model.addAttribute("order", order);
            return "admin/order/detail";
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
            return "redirect:/admin/order";
        }
    }

    /** A24：发货（仅 PAID） */
    @PostMapping("/admin/order/{id}/ship")
    @OperLog(module = "订单管理", action = "发货")
    @ResponseBody
    public Result<Void> ship(@PathVariable Long id,
                             @Valid @ModelAttribute("shipForm") ShipForm shipForm,
                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Result.fail(ResultCode.FAIL.getCode(), firstError(bindingResult));
        }
        adminOrderService.ship(id, shipForm);
        return Result.ok("已发货，订单状态更新为已发货", (Void) null);
    }

    /** A25：修改收货信息与备注 */
    @PostMapping("/admin/order/{id}/receiver")
    @OperLog(module = "订单管理", action = "修改收货信息")
    @ResponseBody
    public Result<Void> updateReceiver(@PathVariable Long id,
                                       @Valid @ModelAttribute("receiverForm") ReceiverForm receiverForm,
                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Result.fail(ResultCode.FAIL.getCode(), firstError(bindingResult));
        }
        adminOrderService.updateReceiver(id, receiverForm);
        return Result.ok("收货信息已更新", (Void) null);
    }

    /** A26：删除（仅终态） */
    @DeleteMapping("/admin/order/{id}")
    @OperLog(module = "订单管理", action = "删除订单")
    @ResponseBody
    public Result<Void> remove(@PathVariable Long id) {
        adminOrderService.remove(id);
        return Result.ok("订单已删除", (Void) null);
    }

    // ------------------------------------------------------------------ 内部方法

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().isEmpty()
                ? ResultCode.FAIL.getMessage()
                : bindingResult.getAllErrors().get(0).getDefaultMessage();
    }

    /** 状态下拉：code 用枚举名，label 用中文描述（避免模板里直接调枚举方法） */
    private List<Map<String, Object>> statusOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("code", status.name());
            option.put("label", status.getDesc());
            options.add(option);
        }
        return options;
    }
}
