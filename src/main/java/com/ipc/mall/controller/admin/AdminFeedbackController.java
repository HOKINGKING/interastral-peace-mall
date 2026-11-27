package com.ipc.mall.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.annotation.OperLog;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.common.util.PageUtils;
import com.ipc.mall.dto.FeedbackCloseForm;
import com.ipc.mall.dto.FeedbackReplyForm;
import com.ipc.mall.service.AdminFeedbackService;
import com.ipc.mall.vo.AdminFeedbackRowVO;
import com.ipc.mall.vo.FeedbackDetailVO;
import com.ipc.mall.vo.LoginAdmin;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 后台投诉与反馈管理（PRD §6.6 / docs/api.md A28–A32）。
 * <p>
 * 列表/详情返回视图；受理、回复、关闭返回 {@code Result}（页面用 fetch 提交），
 * 三个写操作都记操作日志，模块「售后管理」。
 */
@Controller
public class AdminFeedbackController {

    private static final int PAGE_SIZE = 10;

    private final AdminFeedbackService adminFeedbackService;

    public AdminFeedbackController(AdminFeedbackService adminFeedbackService) {
        this.adminFeedbackService = adminFeedbackService;
    }

    /** A28：列表（类型/状态/订单号/用户名/时间区间；PENDING 优先） */
    @GetMapping("/admin/feedback")
    public String list(@RequestParam(required = false) String type,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String orderNo,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                       @RequestParam(required = false)
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                       @RequestParam(required = false, defaultValue = "1") Integer page,
                       Model model) {
        IPage<AdminFeedbackRowVO> feedbacks = adminFeedbackService.page(type, status, orderNo, username,
                dateFrom, dateTo, page, PAGE_SIZE);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", type);
        params.put("status", status);
        params.put("orderNo", orderNo);
        params.put("username", username);
        params.put("dateFrom", dateFrom);
        params.put("dateTo", dateTo);

        model.addAttribute("pageTitle", "投诉与反馈");
        model.addAttribute("feedbacks", feedbacks.getRecords());
        model.addAttribute("total", feedbacks.getTotal());
        model.addAttribute("pagination", PageUtils.pagination(feedbacks, "/admin/feedback", params));
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("orderNo", orderNo);
        model.addAttribute("username", username);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        return "admin/feedback/list";
    }

    /** A29：详情（内容、补充说明、关联订单摘要、状态时间线） */
    @GetMapping("/admin/feedback/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            FeedbackDetailVO feedback = adminFeedbackService.detail(id);
            model.addAttribute("pageTitle", "投诉与反馈详情");
            model.addAttribute("feedback", feedback);
            return "admin/feedback/detail";
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
            return "redirect:/admin/feedback";
        }
    }

    /** A30：受理 PENDING → PROCESSING */
    @PostMapping("/admin/feedback/{id}/accept")
    @OperLog(module = "售后管理", action = "受理投诉")
    @ResponseBody
    public Result<Void> accept(@PathVariable Long id) {
        adminFeedbackService.accept(id);
        return Result.ok("已受理，状态更新为处理中", (Void) null);
    }

    /** A31：回复，写回复人与回复时间 */
    @PostMapping("/admin/feedback/{id}/reply")
    @OperLog(module = "售后管理", action = "回复投诉")
    @ResponseBody
    public Result<Void> reply(@PathVariable Long id,
                              @Valid @ModelAttribute("replyForm") FeedbackReplyForm replyForm,
                              BindingResult bindingResult,
                              HttpSession session) {
        if (bindingResult.hasErrors()) {
            return Result.fail(ResultCode.FAIL.getCode(), firstError(bindingResult));
        }
        adminFeedbackService.reply(id, replyForm.getReply(), currentAdminId(session));
        return Result.ok("已回复，等待用户确认", (Void) null);
    }

    /** A32：关闭（未关闭状态均可），写关闭原因 */
    @PostMapping("/admin/feedback/{id}/close")
    @OperLog(module = "售后管理", action = "关闭投诉")
    @ResponseBody
    public Result<Void> close(@PathVariable Long id,
                              @Valid @ModelAttribute("closeForm") FeedbackCloseForm closeForm,
                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Result.fail(ResultCode.FAIL.getCode(), firstError(bindingResult));
        }
        adminFeedbackService.close(id, closeForm.getCloseReason());
        return Result.ok("已关闭", (Void) null);
    }

    // ------------------------------------------------------------------ 内部方法

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().isEmpty()
                ? ResultCode.FAIL.getMessage()
                : bindingResult.getAllErrors().get(0).getDefaultMessage();
    }

    private Long currentAdminId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SessionKey.LOGIN_ADMIN);
        return value instanceof LoginAdmin admin ? admin.getAdminId() : null;
    }
}
