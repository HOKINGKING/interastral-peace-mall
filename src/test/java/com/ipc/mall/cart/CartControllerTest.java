package com.ipc.mall.cart;

import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.front.CartController;
import com.ipc.mall.service.CartService;
import com.ipc.mall.vo.CartItemVO;
import com.ipc.mall.vo.CartVO;
import com.ipc.mall.vo.LoginAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 购物车页面与接口（F15–F18）。
 * <p>
 * 登录态过滤在拦截器里，这里只验证控制器自身：会话缺失时的兜底跳转、
 * 归属取会话、业务失败经 GlobalExceptionHandler 转成统一 Result。
 */
class CartControllerTest {

    private CartService cartService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ------------------------------------------------------------ F15 页面

    @Test
    void 未登录访问购物车跳登录() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void 购物车页渲染列表与合计() throws Exception {
        CartItemVO item = new CartItemVO();
        item.setCartId(3L);
        item.setItemId(4L);
        item.setProductId(4L);
        item.setProductName("驾驶员徽章套装（6 枚）");
        item.setSpec("6 枚装");
        item.setPrice(new BigDecimal("39.00"));
        item.setPriceText("39.00");
        item.setQuantity(2);
        item.setStock(240);
        item.setMaxQty(99);
        item.setSubtotal(new BigDecimal("78.00"));
        item.setSubtotalText("78.00");
        item.setAvailable(true);

        CartVO cart = new CartVO();
        cart.setItems(List.of(item));
        cart.setTotalCount(2);
        cart.setTotalAmount(new BigDecimal("78.00"));
        when(cartService.list(1L)).thenReturn(cart);

        mockMvc.perform(get("/cart").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/cart/index"))
                .andExpect(model().attribute("pageTitle", "购物车"))
                .andExpect(model().attribute("cart", cart));

        verify(cartService).list(1L);
    }

    // ------------------------------------------------------------ F16 加购

    @Test
    void 加入购物车返回成功提示() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .param("itemId", "6")
                        .param("quantity", "2")
                        .session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已加入购物车"));

        verify(cartService).add(1L, 6L, 2);
    }

    @Test
    void 未登录调用加购返回四百零一() throws Exception {
        mockMvc.perform(post("/cart/add").param("itemId", "6").param("quantity", "1"))
                .andExpect(jsonPath("$.code").value(ResultCode.UNAUTHORIZED.getCode()));

        verify(cartService, never()).add(any(), any(), anyInt());
    }

    @Test
    void 库存不足返回两千零二() throws Exception {
        doThrow(new BizException(ResultCode.STOCK_NOT_ENOUGH))
                .when(cartService).add(eq(1L), eq(6L), eq(99));

        mockMvc.perform(post("/cart/add")
                        .param("itemId", "6")
                        .param("quantity", "99")
                        .session(loginSession()))
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }

    @Test
    void 商品已下架返回两千零一() throws Exception {
        doThrow(new BizException(ResultCode.PRODUCT_OFF_SALE))
                .when(cartService).add(eq(1L), eq(10L), anyInt());

        mockMvc.perform(post("/cart/add")
                        .param("itemId", "10")
                        .param("quantity", "1")
                        .session(loginSession()))
                .andExpect(jsonPath("$.code").value(2001))
                .andExpect(jsonPath("$.message").value("商品不存在或已下架"));
    }

    // ------------------------------------------------------------ F17 / F18

    @Test
    void 修改数量返回成功() throws Exception {
        mockMvc.perform(put("/cart/2")
                        .param("quantity", "3")
                        .session(loginSession()))
                .andExpect(jsonPath("$.code").value(200));

        verify(cartService).updateQuantity(1L, 2L, 3);
    }

    @Test
    void 修改数量越界返回四百() throws Exception {
        doThrow(new BizException(ResultCode.FAIL.getCode(), "商品数量需在 1–99 之间"))
                .when(cartService).updateQuantity(eq(1L), eq(2L), eq(100));

        mockMvc.perform(put("/cart/2")
                        .param("quantity", "100")
                        .session(loginSession()))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void 删除返回成功() throws Exception {
        mockMvc.perform(delete("/cart/2").session(loginSession()))
                .andExpect(jsonPath("$.code").value(200));

        verify(cartService).remove(1L, 2L);
    }

    @Test
    void 删除别人的购物车行返回三千零四() throws Exception {
        doThrow(new BizException(ResultCode.NO_PERMISSION)).when(cartService).remove(2L, 2L);

        mockMvc.perform(delete("/cart/2").session(loginSession(2L, "bob", "陈开拓")))
                .andExpect(jsonPath("$.code").value(3004))
                .andExpect(jsonPath("$.message").value("无权操作该资源"));
    }

    // ------------------------------------------------------------ 角标

    @Test
    void 角标接口返回商品总件数() throws Exception {
        when(cartService.count(1L)).thenReturn(3);

        mockMvc.perform(get("/cart/count").session(loginSession()))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(3));
    }

    // ------------------------------------------------------------ 夹具

    private MockHttpSession loginSession() {
        return loginSession(1L, "xingye", "星野");
    }

    private MockHttpSession loginSession(Long accountId, String username, String nickname) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ACCOUNT, new LoginAccount(accountId, username, nickname));
        return session;
    }
}
