package com.ipc.mall.controller.front;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.common.constant.FeedbackType;
import com.ipc.mall.common.constant.FeedbackStatus;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.util.PageUtils;
import com.ipc.mall.dto.FeedbackForm;
import com.ipc.mall.dto.FeedbackNoteForm;
import com.ipc.mall.service.FeedbackService;
import com.ipc.mall.vo.FeedbackDetailVO;
import com.ipc.mall.vo.FeedbackFormVO;
import com.ipc.mall.vo.FeedbackRowVO;
import com.ipc.mall.vo.LoginAccount;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 前台投诉与反馈（PRD §5.15 / docs/api.md F32–F39）。
 * <p>
 * 页面流程：提交/撤销/补充说明/确认解决一律 PRG 重定向，失败把原因放进 flash，
 * 只有表单本身重新渲染时保留用户已填内容。
 */
@Controller
public class FeedbackController {

    private static final int PAGE_SIZE = 8;

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // ------------------------------------------------------------------ F32 表单

    @GetMapping("/feedback/form")
    public String form(@RequestParam(required = false) Long orderId,
                       HttpSession session,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        try {
            FeedbackFormVO context = feedbackService.formContext(login.getAccountId(), orderId);
            model.addAttribute("form", context);
            model.addAttribute("feedbackForm", new FeedbackForm());
            model.addAttribute("pageTitle", context.isComplaint() ? "订单投诉" : "意见反馈");
            return "front/feedback/form";
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
            return "redirect:/feedback/list";
        }
    }

    // ------------------------------------------------------------------ F33 提交

    @PostMapping("/feedback/submit")
    public String submit(@Valid @ModelAttribute("feedbackForm") FeedbackForm feedbackForm,
                         BindingResult bindingResult,
                         HttpSession session,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            return renderForm(login, feedbackForm.getOrderId(), model, firstError(bindingResult));
        }
        try {
            feedbackService.submit(login.getAccountId(), feedbackForm);
            redirectAttributes.addFlashAttribute("listMessage", "已提交，卖家会尽快处理");
            return "redirect:/feedback/list";
        } catch (BizException e) {
            return renderForm(login, feedbackForm.getOrderId(), model, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ F34 列表

    @GetMapping("/feedback/list")
    public String list(@RequestParam(required = false) String type,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false, defaultValue = "1") Integer page,
                       HttpSession session,
                       Model model) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        String currentType = normalizeType(type);
        String currentStatus = normalizeStatus(status);
        IPage<FeedbackRowVO> feedbacks = feedbackService.pageMine(login.getAccountId(),
                currentType, currentStatus, page, PAGE_SIZE);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", currentType);
        params.put("status", currentStatus);

        model.addAttribute("pageTitle", "我的投诉与反馈");
        model.addAttribute("feedbacks", feedbacks.getRecords());
        model.addAttribute("total", feedbacks.getTotal());
        model.addAttribute("pagination", PageUtils.pagination(feedbacks, "/feedback/list", params));
        model.addAttribute("typeTabs", typeTabs(currentType));
        model.addAttribute("type", currentType);
        model.addAttribute("status", currentStatus);
        return "front/feedback/list";
    }

    // ------------------------------------------------------------------ F35 详情

    @GetMapping("/feedback/{id}")
    public String detail(@PathVariable Long id,
                         HttpSession session,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        try {
            FeedbackDetailVO feedback = feedbackService.detail(login.getAccountId(), id);
            model.addAttribute("pageTitle", feedback.getTypeDesc() + "详情");
            model.addAttribute("feedback", feedback);
            return "front/feedback/detail";
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
            return "redirect:/feedback/list";
        }
    }

    // ------------------------------------------------------------------ F36 撤销

    @PostMapping("/feedback/{id}/cancel")
    public String cancel(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        try {
            feedbackService.cancel(login.getAccountId(), id);
            redirectAttributes.addFlashAttribute("listMessage", "已撤销，卖家将不再处理");
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
        }
        return "redirect:/feedback/" + id;
    }

    // ------------------------------------------------------------------ F38 补充说明

    @PostMapping("/feedback/{id}/note")
    public String addNote(@PathVariable Long id,
                          @Valid @ModelAttribute("noteForm") FeedbackNoteForm noteForm,
                          BindingResult bindingResult,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("listMessage", firstError(bindingResult));
            return "redirect:/feedback/" + id;
        }
        try {
            feedbackService.addNote(login.getAccountId(), id, noteForm.getNote());
            redirectAttributes.addFlashAttribute("listMessage", "补充说明已提交");
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
        }
        return "redirect:/feedback/" + id;
    }

    // ------------------------------------------------------------------ F39 确认已解决

    @PostMapping("/feedback/{id}/resolve")
    public String resolve(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        try {
            feedbackService.resolve(login.getAccountId(), id);
            redirectAttributes.addFlashAttribute("listMessage", "已确认解决，感谢你的反馈");
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
        }
        return "redirect:/feedback/" + id;
    }

    // ------------------------------------------------------------------ 内部方法

    private String renderForm(LoginAccount login, Long orderId, Model model, String error) {
        try {
            FeedbackFormVO context = feedbackService.formContext(login.getAccountId(), orderId);
            model.addAttribute("form", context);
            model.addAttribute("formError", error);
            model.addAttribute("pageTitle", context.isComplaint() ? "订单投诉" : "意见反馈");
            return "front/feedback/form";
        } catch (BizException e) {
            return "redirect:/feedback/list";
        }
    }

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().isEmpty()
                ? "请检查填写内容"
                : bindingResult.getAllErrors().get(0).getDefaultMessage();
    }

    private List<Map<String, Object>> typeTabs(String current) {
        List<Map<String, Object>> tabs = new ArrayList<>();
        tabs.add(tab(null, "全部", current == null));
        tabs.add(tab(FeedbackType.COMPLAINT, "投诉", FeedbackType.COMPLAINT.equals(current)));
        tabs.add(tab(FeedbackType.FEEDBACK, "反馈", FeedbackType.FEEDBACK.equals(current)));
        return tabs;
    }

    private Map<String, Object> tab(String code, String label, boolean active) {
        Map<String, Object> tab = new LinkedHashMap<>();
        tab.put("code", code);
        tab.put("label", label);
        tab.put("active", active);
        tab.put("url", code == null ? "/feedback/list" : "/feedback/list?type=" + code);
        return tab;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String value = type.trim();
        return FeedbackType.COMPLAINT.equals(value) || FeedbackType.FEEDBACK.equals(value) ? value : null;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim();
        for (FeedbackStatus candidate : FeedbackStatus.values()) {
            if (candidate.name().equals(value)) {
                return value;
            }
        }
        return null;
    }

    private LoginAccount currentAccount(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SessionKey.LOGIN_ACCOUNT);
        return value instanceof LoginAccount account ? account : null;
    }
}
