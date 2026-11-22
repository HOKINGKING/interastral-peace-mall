package com.ipc.mall.controller.front;

import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.service.CartService;
import com.ipc.mall.vo.CartVO;
import com.ipc.mall.vo.LoginAccount;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 购物车（PRD §5.9 / docs/api.md F15–F18）。
 * <p>
 * F15 返回视图，F16–F18 返回 {@code Result}：购物车页与详情页都用 fetch 调用，
 * 失败信息靠 {@link com.ipc.mall.common.exception.GlobalExceptionHandler} 转成统一 JSON，
 * 页面只负责 toast，不在这里 try-catch。
 * <p>
 * 归属一律取自会话里的 {@link LoginAccount}，<b>不接收</b>前端传的 accountId。
 */
@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** F15：购物车视图（勾选合计、失效商品置灰由页面脚本处理） */
    @GetMapping("/cart")
    public String index(HttpSession session, Model model) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        CartVO cart = cartService.list(login.getAccountId());
        model.addAttribute("pageTitle", "购物车");
        model.addAttribute("cart", cart);
        return "front/cart/index";
    }

    /**
     * 顶部角标刷新用的轻量接口（非 PRD 接口，仅前端加购/改数量/删除后回填数字）。
     * 页面首屏的角标由 {@code FrontModelAdvice#cartCount} 直接渲染，不走这里。
     */
    @GetMapping("/cart/count")
    @ResponseBody
    public Result<Integer> count(HttpSession session) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        return Result.ok(cartService.count(login.getAccountId()));
    }

    /** F16：加入购物车，同 SKU 累加 / 已删除复活 / 下架 2001 / 库存不足 2002 */
    @PostMapping("/cart/add")
    @ResponseBody
    public Result<Void> add(@RequestParam(required = false) Long itemId,
                            @RequestParam(required = false) Integer quantity,
                            HttpSession session) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        cartService.add(login.getAccountId(), itemId, quantity == null ? 1 : quantity);
        return Result.ok("已加入购物车", (Void) null);
    }

    /** F17：修改数量（1–99 且 ≤ 库存） */
    @PutMapping("/cart/{id}")
    @ResponseBody
    public Result<Void> updateQuantity(@PathVariable Long id,
                                       @RequestParam(required = false) Integer quantity,
                                       HttpSession session) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        cartService.updateQuantity(login.getAccountId(), id, quantity == null ? 0 : quantity);
        return Result.ok("数量已更新", (Void) null);
    }

    /** F18：删除（逻辑删除，非本人 3004） */
    @DeleteMapping("/cart/{id}")
    @ResponseBody
    public Result<Void> remove(@PathVariable Long id, HttpSession session) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        cartService.remove(login.getAccountId(), id);
        return Result.ok("已从购物车移除", (Void) null);
    }

    private LoginAccount currentAccount(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(SessionKey.LOGIN_ACCOUNT);
        return value instanceof LoginAccount account ? account : null;
    }
}
