package com.ipc.mall.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.common.constant.AccountStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.common.util.PageUtils;
import com.ipc.mall.dto.AdminAccountForm;
import com.ipc.mall.dto.AdminResetPasswordForm;
import com.ipc.mall.service.AccountAdminService;
import com.ipc.mall.vo.AdminAccountDetailVO;
import com.ipc.mall.vo.AdminAccountRowVO;
import jakarta.validation.Valid;
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 后台用户管理（PRD §6.4 / docs/api.md A18–A21）。
 * <p>
 * 列表/详情返回视图；编辑与重置密码返回 {@code Result}（页面用 fetch 提交，与其它后台模块一致）。
 * 登录态由 {@code AdminLoginInterceptor} 统一拦截，这里不再重复判断。
 */
@Controller
public class AdminAccountController {

    private static final int PAGE_SIZE = 10;

    private final AccountAdminService accountAdminService;

    public AdminAccountController(AccountAdminService accountAdminService) {
        this.accountAdminService = accountAdminService;
    }

    /** A18：用户列表（用户名 / 昵称 / 手机号关键词 + 状态筛选） */
    @GetMapping("/admin/account")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false, defaultValue = "1") Integer page,
                       Model model) {
        Integer statusFilter = normalizeStatus(status);
        IPage<AdminAccountRowVO> accounts = accountAdminService.page(keyword, statusFilter, page, PAGE_SIZE);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", keyword);
        params.put("status", statusFilter);

        model.addAttribute("pageTitle", "用户管理");
        model.addAttribute("accounts", accounts.getRecords());
        model.addAttribute("total", accounts.getTotal());
        model.addAttribute("pagination", PageUtils.pagination(accounts, "/admin/account", params));
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", statusFilter);
        return "admin/account/list";
    }

    /** A19：用户详情（含订单数、编辑表单、重置密码） */
    @GetMapping("/admin/account/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            AdminAccountDetailVO account = accountAdminService.detail(id);
            model.addAttribute("pageTitle", "用户详情");
            model.addAttribute("account", account);
            return "admin/account/detail";
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
            return "redirect:/admin/account";
        }
    }

    /** A20：保存昵称 / 手机号 / 状态 */
    @PostMapping("/admin/account/save")
    @ResponseBody
    public Result<Void> save(@Valid @ModelAttribute("accountForm") AdminAccountForm accountForm,
                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Result.fail(ResultCode.FAIL.getCode(), firstError(bindingResult));
        }
        accountAdminService.save(accountForm);
        return Result.ok("用户信息已更新", (Void) null);
    }

    /** A21：重置密码（BCrypt 入库，旧密码立即失效） */
    @PostMapping("/admin/account/{id}/reset-password")
    @ResponseBody
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @Valid @ModelAttribute("resetPasswordForm") AdminResetPasswordForm form,
                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return Result.fail(ResultCode.FAIL.getCode(), firstError(bindingResult));
        }
        accountAdminService.resetPassword(id, form.getPassword());
        return Result.ok("密码已重置，请通知用户使用新密码登录", (Void) null);
    }

    // ------------------------------------------------------------------ 内部方法

    private String firstError(BindingResult bindingResult) {
        return bindingResult.getAllErrors().isEmpty()
                ? ResultCode.FAIL.getMessage()
                : bindingResult.getAllErrors().get(0).getDefaultMessage();
    }

    /** 只接受 0 / 1；非法值按“全部”处理，避免把脏参数传给 SQL */
    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return status == AccountStatus.ENABLED || status == AccountStatus.DISABLED ? status : null;
    }
}
