package com.ipc.mall.controller.front;

import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.service.CartService;
import com.ipc.mall.service.CategoryService;
import com.ipc.mall.vo.CategoryGroupVO;
import com.ipc.mall.vo.LoginAccount;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * 前台公共模型：每个前台页面都要渲染顶部导航，统一在这里提供分类树，
 * 避免每个 Controller 各写一遍、漏写导致导航为空。
 * <p>
 * 顶部的购物车角标也挂在这里（PRD §5.9）：它对所有前台页面生效，
 * 所以只查一个 SUM，未登录直接返回 0，不碰数据库。
 */
@Slf4j
@ControllerAdvice(basePackages = "com.ipc.mall.controller.front")
public class FrontModelAdvice {

    private final CategoryService categoryService;
    private final CartService cartService;

    public FrontModelAdvice(CategoryService categoryService, CartService cartService) {
        this.categoryService = categoryService;
        this.cartService = cartService;
    }

    @ModelAttribute("navGroups")
    public List<CategoryGroupVO> navGroups() {
        return categoryService.visibleGroups();
    }

    /** 购物车角标：商品总件数，未登录为 0 */
    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session) {
        Object value = session.getAttribute(SessionKey.LOGIN_ACCOUNT);
        if (!(value instanceof LoginAccount login)) {
            return 0;
        }
        try {
            return cartService.count(login.getAccountId());
        } catch (RuntimeException e) {
            // 角标只是锦上添花，查挂了也不该让整个页面 500
            log.warn("购物车角标查询失败，accountId={}: {}", login.getAccountId(), e.getMessage());
            return 0;
        }
    }
}
