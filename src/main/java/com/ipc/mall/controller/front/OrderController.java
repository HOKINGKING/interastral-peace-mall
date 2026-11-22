package com.ipc.mall.controller.front;

import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.dto.OrderSubmitForm;
import com.ipc.mall.service.OrderService;
import com.ipc.mall.vo.CheckoutVO;
import com.ipc.mall.vo.LoginAccount;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * 前台交易（PRD §5.11–§5.14 / docs/api.md F24–F31、F37）。
 * <p>
 * 全部是页面流程：成功一律 PRG 重定向，失败把原因放进 flash 再回确认页，
 * 不把页面错误做成 JSON（JSON 只用给二次确认类的 fetch 接口）。
 */
@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ------------------------------------------------------------------ F24 确认订单

    /**
     * 确认订单页：购物车勾选（cartIds）或立即购买（itemId + quantity）。
     * 每次进入都刷新 Session 里的 submitToken（PRD §5.11 防重复提交）。
     */
    @GetMapping("/order/checkout")
    public String checkout(@RequestParam(required = false) String cartIds,
                           @RequestParam(required = false) Long itemId,
                           @RequestParam(required = false) Integer quantity,
                           HttpSession session,
                           Model model) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        CheckoutVO checkout = orderService.checkout(login.getAccountId(), cartIds, itemId, quantity);
        String token = UUID.randomUUID().toString().replace("-", "");
        session.setAttribute(SessionKey.SUBMIT_TOKEN, token);
        checkout.setSubmitToken(token);

        model.addAttribute("pageTitle", "确认订单");
        model.addAttribute("checkout", checkout);
        return "front/order/checkout";
    }

    // ------------------------------------------------------------------ F25 提交订单

    @PostMapping("/order/submit")
    public String submit(@ModelAttribute OrderSubmitForm form,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        if (!verifySubmitToken(session, form.getSubmitToken())) {
            // Token 一次性：取不出来 / 不匹配都说明是重复提交或页面过期
            redirectAttributes.addFlashAttribute("listMessage", "请勿重复提交，已为你打开订单列表");
            return "redirect:/order/list";
        }
        try {
            Long orderId = orderService.submit(login.getAccountId(), form);
            return "redirect:/order/" + orderId + "/pay";
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("checkoutError", e.getMessage());
            addIfPresent(redirectAttributes, "cartIds", form.getCartIds());
            addIfPresent(redirectAttributes, "itemId", form.getItemId());
            addIfPresent(redirectAttributes, "quantity", form.getQuantity());
            return "redirect:/order/checkout";
        }
    }

    // ------------------------------------------------------------------ 内部方法

    /** 取出即删：无论成功失败都失效，防止一次 Token 反复提交 */
    private boolean verifySubmitToken(HttpSession session, String submitted) {
        Object expected = session.getAttribute(SessionKey.SUBMIT_TOKEN);
        session.removeAttribute(SessionKey.SUBMIT_TOKEN);
        return expected instanceof String value && StringUtils.hasText(submitted) && value.equals(submitted);
    }

    private void addIfPresent(RedirectAttributes redirectAttributes, String key, Object value) {
        if (value != null && (!(value instanceof String text) || StringUtils.hasText(text))) {
            redirectAttributes.addAttribute(key, value);
        }
    }

    private LoginAccount currentAccount(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(SessionKey.LOGIN_ACCOUNT);
        return value instanceof LoginAccount account ? account : null;
    }
}
