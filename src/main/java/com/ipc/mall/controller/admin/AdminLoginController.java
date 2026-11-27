package com.ipc.mall.controller.admin;

import com.ipc.mall.annotation.OperLog;
import com.ipc.mall.common.constant.CaptchaScope;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.util.AuthWebUtils;
import com.ipc.mall.common.util.OperLogContext;
import com.ipc.mall.dto.LoginForm;
import com.ipc.mall.service.AdminService;
import com.ipc.mall.service.CaptchaService;
import com.ipc.mall.vo.LoginAdmin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 后台登录 / 退出（PRD §6.1）。
 * <p>
 * 与前台共用 JSESSIONID，但只使用 LOGIN_ADMIN key，不触碰 LOGIN_ACCOUNT。
 */
@Controller
@RequestMapping("/admin")
public class AdminLoginController {

    private final AdminService adminService;
    private final CaptchaService captchaService;

    public AdminLoginController(AdminService adminService, CaptchaService captchaService) {
        this.adminService = adminService;
        this.captchaService = captchaService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "redirect", required = false) String redirect,
                            HttpSession session,
                            Model model) {
        if (session.getAttribute(SessionKey.LOGIN_ADMIN) != null) {
            return "redirect:/admin";
        }
        LoginForm form = new LoginForm();
        form.setRedirect(redirect);
        model.addAttribute("adminLoginForm", form);
        return "admin/login";
    }

    @PostMapping("/login")
    @OperLog(module = "认证", action = "后台登录", login = true)
    public String login(@Valid @ModelAttribute("adminLoginForm") LoginForm form,
                        BindingResult bindingResult,
                        HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            OperLogContext.markFailure(request, "登录参数校验失败");
            return "admin/login";
        }

        HttpSession session = request.getSession();
        if (!captchaService.verify(session, CaptchaScope.ADMIN, form.getCaptcha())) {
            OperLogContext.markFailure(request, "验证码错误或已过期");
            bindingResult.rejectValue("captcha", "captcha.invalid", "验证码错误或已过期");
            return "admin/login";
        }

        try {
            LoginAdmin loginAdmin = adminService.login(form.getUsername(), form.getPassword());
            request.changeSessionId();
            session.setAttribute(SessionKey.LOGIN_ADMIN, loginAdmin);
            return "redirect:" + safeAdminRedirect(form.getRedirect());
        } catch (BizException e) {
            OperLogContext.markFailure(request, e.getMessage());
            bindingResult.reject("adminLogin.error", e.getMessage());
            return "admin/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SessionKey.LOGIN_ADMIN);
            session.removeAttribute(SessionKey.LOGIN_ADMIN_CHECK_AT);
            session.removeAttribute(SessionKey.ADMIN_CAPTCHA_CODE);
            session.removeAttribute(SessionKey.ADMIN_CAPTCHA_EXPIRE_AT);
        }
        return "redirect:/admin/login";
    }

    /** 后台只允许跳回 /admin 下的页面，避免登录后被带到前台页面 */
    private String safeAdminRedirect(String target) {
        String redirect = AuthWebUtils.safeRedirect(target);
        if (!redirect.equals("/admin") && !redirect.startsWith("/admin/")) {
            return "/admin";
        }
        if (redirect.startsWith("/admin/login")) {
            return "/admin";
        }
        return redirect;
    }
}
