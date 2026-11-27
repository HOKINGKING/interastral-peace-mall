package com.ipc.mall.controller.front;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.common.util.PageUtils;
import com.ipc.mall.dto.OrderSubmitForm;
import com.ipc.mall.entity.Order;
import com.ipc.mall.service.OrderService;
import com.ipc.mall.vo.CheckoutVO;
import com.ipc.mall.vo.LoginAccount;
import com.ipc.mall.vo.OrderSummaryVO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 前台交易（PRD §5.11–§5.14 / docs/api.md F24–F31、F37）。
 * <p>
 * 全部是页面流程：成功一律 PRG 重定向，失败把原因放进 flash 再回确认页，
 * 不把页面错误做成 JSON（JSON 只用给二次确认类的 fetch 接口）。
 */
@Controller
public class OrderController {

    /** 支付倒计时窗口（分钟），与定时关单共用 mall.order.pay-timeout-minutes；默认 30 分钟 */
    @Value("${mall.order.pay-timeout-minutes:30}")
    private long payTimeoutMinutes = 30;

    /** 订单列表每页条数 */
    private static final int PAGE_SIZE = 6;

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
        // 从确认页去新增地址时带上返回地址，保存后回到同一笔结算（保留 cartIds / itemId+quantity）
        model.addAttribute("checkoutReturnUrl", checkoutReturnUrl(cartIds, itemId, quantity));
        return "front/order/checkout";
    }

    /** 拼接确认订单页的完整路径，作为地址表单的 returnTo */
    private String checkoutReturnUrl(String cartIds, Long itemId, Integer quantity) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/order/checkout");
        if (StringUtils.hasText(cartIds)) {
            builder.queryParam("cartIds", cartIds);
        } else if (itemId != null) {
            builder.queryParam("itemId", itemId);
            builder.queryParam("quantity", quantity == null ? 1 : quantity);
        }
        return builder.build().encode().toUriString();
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

    // ------------------------------------------------------------------ F26 支付页

    @GetMapping("/order/{id}/pay")
    public String payPage(@PathVariable Long id,
                          HttpSession session,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        try {
            Order order = orderService.get(login.getAccountId(), id);
            if (!order.isPendingPayment()) {
                redirectAttributes.addFlashAttribute("listMessage", "该订单已不在待支付状态，无需再次支付");
                return "redirect:/order/list";
            }
            model.addAttribute("pageTitle", "订单支付");
            model.addAttribute("order", order);
            model.addAttribute("remainSeconds", remainSeconds(order));
            return "front/order/pay";
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
            return "redirect:/order/list";
        }
    }

    // ------------------------------------------------------------------ F27 模拟支付

    @PostMapping("/order/{id}/pay")
    public String pay(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        try {
            orderService.pay(login.getAccountId(), id);
            redirectAttributes.addFlashAttribute("listMessage", "支付成功，感谢购买");
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
        }
        return "redirect:/order/" + id;
    }

    // ------------------------------------------------------------------ F28 取消订单

    @PostMapping("/order/{id}/cancel")
    public String cancel(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        try {
            orderService.cancel(login.getAccountId(), id);
            redirectAttributes.addFlashAttribute("listMessage", "订单已取消，库存已回补");
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
        }
        return "redirect:/order/" + id;
    }

    // ------------------------------------------------------------------ F29 订单列表

    @GetMapping("/order/list")
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false, defaultValue = "1") Integer page,
                       HttpSession session,
                       Model model) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        String current = normalizeStatus(status);
        IPage<OrderSummaryVO> orders = orderService.pageOrders(login.getAccountId(), current, page, PAGE_SIZE);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("status", current);
        model.addAttribute("pageTitle", "我的订单");
        model.addAttribute("orders", orders.getRecords());
        model.addAttribute("total", orders.getTotal());
        model.addAttribute("currentStatus", current);
        model.addAttribute("tabs", tabs(current));
        model.addAttribute("pagination", PageUtils.pagination(orders, "/order/list", params));
        return "front/order/list";
    }

    // ------------------------------------------------------------------ F30 订单详情

    @GetMapping("/order/{id}")
    public String detail(@PathVariable Long id,
                         HttpSession session,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        try {
            model.addAttribute("pageTitle", "订单详情");
            model.addAttribute("order", orderService.detail(login.getAccountId(), id));
            return "front/order/detail";
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
            return "redirect:/order/list";
        }
    }

    // ------------------------------------------------------------------ F31 确认收货

    @PostMapping("/order/{id}/receive")
    public String receive(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        try {
            orderService.receive(login.getAccountId(), id);
            redirectAttributes.addFlashAttribute("listMessage", "已确认收货，交易完成");
        } catch (BizException e) {
            redirectAttributes.addFlashAttribute("listMessage", e.getMessage());
        }
        return "redirect:/order/" + id;
    }

    // ------------------------------------------------------------------ F37 删除订单

    @DeleteMapping("/order/{id}")
    @ResponseBody
    public Result<Void> remove(@PathVariable Long id, HttpSession session) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        orderService.remove(login.getAccountId(), id);
        return Result.ok("订单已删除", (Void) null);
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

    /** 状态 Tab：code=null 表示「全部」 */
    private List<Map<String, Object>> tabs(String current) {
        List<Map<String, Object>> tabs = new ArrayList<>();
        tabs.add(tab(null, "全部", current == null));
        for (OrderStatus status : OrderStatus.values()) {
            tabs.add(tab(status.name(), status.getDesc(), status.name().equals(current)));
        }
        return tabs;
    }

    private Map<String, Object> tab(String code, String label, boolean active) {
        Map<String, Object> tab = new LinkedHashMap<>();
        tab.put("code", code);
        tab.put("label", label);
        tab.put("active", active);
        tab.put("url", code == null ? "/order/list" : "/order/list?status=" + code);
        return tab;
    }

    /** 状态筛选：只接受合法枚举名，空/非法按「全部」 */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        for (OrderStatus candidate : OrderStatus.values()) {
            if (candidate.name().equals(status.trim())) {
                return candidate.name();
            }
        }
        return null;
    }

    /** 支付倒计时：下单后 N 分钟（与自动关单同一配置），到点由定时任务关闭 */
    private long remainSeconds(Order order) {
        if (order.getCreateTime() == null) {
            return 0;
        }
        long elapsed = Duration.between(order.getCreateTime(), LocalDateTime.now()).getSeconds();
        long window = Math.max(1, payTimeoutMinutes) * 60;
        return Math.max(0, window - elapsed);
    }

    private LoginAccount currentAccount(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(SessionKey.LOGIN_ACCOUNT);
        return value instanceof LoginAccount account ? account : null;
    }
}
