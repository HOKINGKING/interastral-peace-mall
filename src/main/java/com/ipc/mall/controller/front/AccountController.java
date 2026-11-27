package com.ipc.mall.controller.front;

import com.ipc.mall.annotation.OperLog;
import com.ipc.mall.common.constant.CaptchaScope;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.common.util.AuthWebUtils;
import com.ipc.mall.common.util.OperLogContext;
import com.ipc.mall.dto.ChangePasswordForm;
import com.ipc.mall.dto.LoginForm;
import com.ipc.mall.dto.RegisterForm;
import com.ipc.mall.service.AccountService;
import com.ipc.mall.service.CaptchaService;
import com.ipc.mall.vo.AccountProfileVO;
import com.ipc.mall.vo.LoginAccount;
import com.ipc.mall.vo.PasswordChangeResultVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 前台认证页面（PRD §5.5–5.8）。
 * <p>
 * 页面请求不抛业务异常给全局 REST 异常处理器：预期错误回填 BindingResult，
 * 成功一律 PRG 重定向。
 */
@Controller
public class AccountController {

    private final AccountService accountService;
    private final CaptchaService captchaService;

    public AccountController(AccountService accountService, CaptchaService captchaService) {
        this.accountService = accountService;
        this.captchaService = captchaService;
    }

    // ------------------------------------------------------------ 登录

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "redirect", required = false) String redirect,
                            @RequestParam(value = "changed", required = false) String changed,
                            HttpSession session,
                            Model model) {
        if (session.getAttribute(SessionKey.LOGIN_ACCOUNT) != null) {
            return "redirect:/";
        }
        LoginForm form = new LoginForm();
        form.setRedirect(redirect);
        model.addAttribute("loginForm", form);
        if ("1".equals(changed)) {
            model.addAttribute("message", "密码修改成功，请使用新密码重新登录");
        }
        return "front/login";
    }

    @PostMapping("/user/login")
    @OperLog(module = "认证", action = "前台登录", login = true)
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form,
                        BindingResult bindingResult,
                        HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            OperLogContext.markFailure(request, "登录参数校验失败");
            return "front/login";
        }

        HttpSession session = request.getSession();
        if (!captchaService.verify(session, CaptchaScope.FRONT, form.getCaptcha())) {
            OperLogContext.markFailure(request, "验证码错误或已过期");
            bindingResult.rejectValue("captcha", "captcha.invalid", "验证码错误或已过期");
            return "front/login";
        }

        try {
            LoginAccount loginAccount = accountService.login(form.getUsername(), form.getPassword());
            // 防会话固定：只换 JSESSIONID，不丢同浏览器的后台登录态
            request.changeSessionId();
            session.setAttribute(SessionKey.LOGIN_ACCOUNT, loginAccount);
            return "redirect:" + AuthWebUtils.safeRedirect(form.getRedirect());
        } catch (BizException e) {
            OperLogContext.markFailure(request, e.getMessage());
            bindingResult.reject("login.error", e.getMessage());
            return "front/login";
        }
    }

    // ------------------------------------------------------------ 注册

    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model) {
        if (session.getAttribute(SessionKey.LOGIN_ACCOUNT) != null) {
            return "redirect:/";
        }
        model.addAttribute("registerForm", new RegisterForm());
        return "front/register";
    }

    @PostMapping("/user/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult bindingResult,
                           HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return "front/register";
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "两次输入的密码不一致");
            return "front/register";
        }

        HttpSession session = request.getSession();
        if (!captchaService.verify(session, CaptchaScope.FRONT, form.getCaptcha())) {
            bindingResult.rejectValue("captcha", "captcha.invalid", "验证码错误或已过期");
            return "front/register";
        }

        try {
            LoginAccount loginAccount = accountService.register(form);
            request.changeSessionId();
            session.setAttribute(SessionKey.LOGIN_ACCOUNT, loginAccount);
            return "redirect:/";
        } catch (BizException e) {
            if (e.getCode() == ResultCode.USERNAME_EXISTS.getCode()) {
                bindingResult.rejectValue("username", "username.exists", e.getMessage());
            } else {
                bindingResult.reject("register.error", e.getMessage());
            }
            return "front/register";
        }
    }

    // ------------------------------------------------------------ 退出

    @PostMapping("/user/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            // 只清前台 key，保留 LOGIN_ADMIN，保证前后台会话隔离
            session.removeAttribute(SessionKey.LOGIN_ACCOUNT);
            session.removeAttribute(SessionKey.LOGIN_ACCOUNT_CHECK_AT);
            session.removeAttribute(SessionKey.CAPTCHA_CODE);
            session.removeAttribute(SessionKey.CAPTCHA_EXPIRE_AT);
        }
        return "redirect:/";
    }

    // ------------------------------------------------------------ 个人中心

    @GetMapping("/user/center")
    public String center(HttpSession session, Model model) {
        LoginAccount loginAccount = currentAccount(session);
        if (loginAccount == null) {
            return "redirect:/login";
        }
        try {
            AccountProfileVO profile = accountService.getProfile(loginAccount.getAccountId());
            model.addAttribute("profile", profile);
            return "front/user/center";
        } catch (BizException e) {
            clearFrontLogin(session);
            return "redirect:/login";
        }
    }

    @GetMapping("/user/password")
    public String passwordPage(HttpSession session, Model model) {
        if (currentAccount(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("changePasswordForm", new ChangePasswordForm());
        return "front/user/password";
    }

    @PostMapping("/user/password")
    public String changePassword(@Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
                                 BindingResult bindingResult,
                                 HttpServletRequest request) {
        LoginAccount loginAccount = currentAccount(request.getSession(false));
        if (loginAccount == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            return "front/user/password";
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "两次输入的新密码不一致");
            return "front/user/password";
        }

        try {
            accountService.changePassword(loginAccount.getAccountId(), form);
        } catch (BizException e) {
            if (e.getCode() == ResultCode.OLD_PASSWORD_ERROR.getCode()) {
                bindingResult.rejectValue("oldPassword", "oldPassword.invalid", e.getMessage());
            } else {
                bindingResult.reject("password.error", e.getMessage());
            }
            return "front/user/password";
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            clearFrontLogin(session);
        }
        // 只轮换 ID，保留后台登录态
        request.changeSessionId();
        return "redirect:/login?changed=1";
    }

    /**
     * 修改密码的异步版本（PRD §5.8）：前端 fetch 调用，错误只刷新提示文字，不整页刷新。
     * <p>
     * 返回体里的 {@code data.field} 告诉前端该把错误显示在哪个输入框下面；
     * 新密码相关错误由前端清空「新密码 / 确认新密码」两栏，原密码保留以便重试。
     */
    @PostMapping(value = "/user/password/async", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Result<PasswordChangeResultVO> changePasswordAsync(
            @Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
            BindingResult bindingResult,
            HttpServletRequest request) {
        LoginAccount loginAccount = currentAccount(request.getSession(false));
        if (loginAccount == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            String field = fieldError == null ? "newPassword" : fieldError.getField();
            String message = fieldError == null ? "请检查输入" : fieldError.getDefaultMessage();
            return passwordFail(ResultCode.FAIL.getCode(), field, message);
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            return passwordFail(ResultCode.FAIL.getCode(), "confirmPassword", "两次输入的新密码不一致");
        }

        try {
            accountService.changePassword(loginAccount.getAccountId(), form);
        } catch (BizException e) {
            String field = e.getCode() == ResultCode.OLD_PASSWORD_ERROR.getCode()
                    ? "oldPassword"
                    : "newPassword";
            return passwordFail(e.getCode(), field, e.getMessage());
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            clearFrontLogin(session);
        }
        // 只轮换 ID，保留后台登录态
        request.changeSessionId();
        return Result.ok("密码修改成功，请使用新密码重新登录",
                PasswordChangeResultVO.redirect("/login?changed=1"));
    }

    private Result<PasswordChangeResultVO> passwordFail(int code, String field, String message) {
        return new Result<>(code, message, PasswordChangeResultVO.field(field));
    }

    private LoginAccount currentAccount(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SessionKey.LOGIN_ACCOUNT);
        return value instanceof LoginAccount account ? account : null;
    }

    private void clearFrontLogin(HttpSession session) {
        session.removeAttribute(SessionKey.LOGIN_ACCOUNT);
        session.removeAttribute(SessionKey.LOGIN_ACCOUNT_CHECK_AT);
    }
}
