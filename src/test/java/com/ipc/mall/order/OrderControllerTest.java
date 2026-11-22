package com.ipc.mall.order;

import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.controller.front.OrderController;
import com.ipc.mall.dto.OrderSubmitForm;
import com.ipc.mall.service.OrderService;
import com.ipc.mall.vo.CheckoutVO;
import com.ipc.mall.vo.LoginAccount;
import com.ipc.mall.vo.OrderItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 结算下单页面流程（F24 / F25）：登录兜底、submitToken 一次性、失败回填。
 */
class OrderControllerTest {

    private OrderService orderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ------------------------------------------------------------ F24

    @Test
    void 未登录访问确认页跳登录() throws Exception {
        mockMvc.perform(get("/order/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void 确认页渲染明细并下发一次性token() throws Exception {
        CheckoutVO checkout = checkoutVO();
        when(orderService.checkout(eq(1L), eq("1,2"), any(), any())).thenReturn(checkout);

        MockHttpSession session = loginSession();
        mockMvc.perform(get("/order/checkout").param("cartIds", "1,2").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("front/order/checkout"))
                .andExpect(model().attribute("pageTitle", "确认订单"))
                .andExpect(model().attribute("checkout", checkout));

        Object token = session.getAttribute(SessionKey.SUBMIT_TOKEN);
        assertNotNull(token, "确认页必须把 submitToken 写进 Session");
        assertNotEquals("", token);
        verify(orderService).checkout(1L, "1,2", null, null);
    }

    @Test
    void 立即购买参数透传到服务层() throws Exception {
        when(orderService.checkout(eq(1L), eq(null), eq(5L), eq(2))).thenReturn(checkoutVO());

        mockMvc.perform(get("/order/checkout").param("itemId", "5").param("quantity", "2")
                        .session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/order/checkout"));

        verify(orderService).checkout(1L, null, 5L, 2);
    }

    // ------------------------------------------------------------ F25

    @Test
    void 提交成功跳支付页并消费token() throws Exception {
        when(orderService.submit(eq(1L), any(OrderSubmitForm.class))).thenReturn(100L);

        MockHttpSession session = loginSession();
        session.setAttribute(SessionKey.SUBMIT_TOKEN, "token-1");

        mockMvc.perform(post("/order/submit")
                        .param("submitToken", "token-1")
                        .param("addressId", "1")
                        .param("cartIds", "1,2")
                        .param("remark", "工作日送达")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/100/pay"));

        assertNull(session.getAttribute(SessionKey.SUBMIT_TOKEN), "token 必须一次性消费");
    }

    @Test
    void 重复提交跳订单列表并提示() throws Exception {
        MockHttpSession session = loginSession();
        session.setAttribute(SessionKey.SUBMIT_TOKEN, "token-1");

        mockMvc.perform(post("/order/submit")
                        .param("submitToken", "token-old")
                        .param("addressId", "1")
                        .param("cartIds", "1,2")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/list"))
                .andExpect(flash().attributeExists("listMessage"));

        verify(orderService, never()).submit(any(), any());
        assertNull(session.getAttribute(SessionKey.SUBMIT_TOKEN));
    }

    @Test
    void 没有token时提交被拒绝() throws Exception {
        mockMvc.perform(post("/order/submit")
                        .param("submitToken", "whatever")
                        .param("addressId", "1")
                        .param("cartIds", "1")
                        .session(loginSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/list"));

        verify(orderService, never()).submit(any(), any());
    }

    @Test
    void 提交失败回确认页并带回原因与参数() throws Exception {
        doThrow(new BizException(ResultCode.STOCK_NOT_ENOUGH.getCode(), "「驾驶员徽章套装（6 枚）」库存不足，仅剩 1 件"))
                .when(orderService).submit(eq(1L), any(OrderSubmitForm.class));

        MockHttpSession session = loginSession();
        session.setAttribute(SessionKey.SUBMIT_TOKEN, "token-1");

        mockMvc.perform(post("/order/submit")
                        .param("submitToken", "token-1")
                        .param("addressId", "1")
                        .param("cartIds", "1,2")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/order/checkout?*"))
                .andExpect(flash().attribute("checkoutError", "「驾驶员徽章套装（6 枚）」库存不足，仅剩 1 件"));
    }

    @Test
    void 未登录提交跳登录() throws Exception {
        mockMvc.perform(post("/order/submit").param("submitToken", "x").param("addressId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ------------------------------------------------------------ F26 支付页

    @Test
    void 待支付订单可以进入支付页() throws Exception {
        when(orderService.get(1L, 8L)).thenReturn(order(8L, OrderStatus.PENDING_PAYMENT));

        mockMvc.perform(get("/order/8/pay").session(loginSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("front/order/pay"))
                .andExpect(model().attribute("pageTitle", "订单支付"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attributeExists("remainSeconds"));
    }

    @Test
    void 非待支付订单进支付页跳列表() throws Exception {
        when(orderService.get(1L, 8L)).thenReturn(order(8L, OrderStatus.PAID));

        mockMvc.perform(get("/order/8/pay").session(loginSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/list"))
                .andExpect(flash().attributeExists("listMessage"));
    }

    @Test
    void 看别人的支付页跳列表并提示() throws Exception {
        when(orderService.get(1L, 8L)).thenThrow(new BizException(ResultCode.ORDER_NOT_OWNED));

        mockMvc.perform(get("/order/8/pay").session(loginSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/list"))
                .andExpect(flash().attribute("listMessage", ResultCode.ORDER_NOT_OWNED.getMessage()));
    }

    // ------------------------------------------------------------ F27 支付

    @Test
    void 模拟支付成功跳订单详情() throws Exception {
        mockMvc.perform(post("/order/8/pay").session(loginSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/8"))
                .andExpect(flash().attributeExists("listMessage"));

        verify(orderService).pay(1L, 8L);
    }

    @Test
    void 支付失败回详情并带原因() throws Exception {
        doThrow(new BizException(ResultCode.ORDER_STATUS_INVALID)).when(orderService).pay(1L, 8L);

        mockMvc.perform(post("/order/8/pay").session(loginSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/8"))
                .andExpect(flash().attribute("listMessage", ResultCode.ORDER_STATUS_INVALID.getMessage()));
    }

    @Test
    void 未登录支付跳登录() throws Exception {
        mockMvc.perform(post("/order/8/pay"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ------------------------------------------------------------ F28 取消

    @Test
    void 取消订单跳订单详情() throws Exception {
        mockMvc.perform(post("/order/8/cancel").session(loginSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/8"))
                .andExpect(flash().attributeExists("listMessage"));

        verify(orderService).cancel(1L, 8L);
    }

    @Test
    void 取消已支付订单回详情并提示状态不允许() throws Exception {
        doThrow(new BizException(ResultCode.ORDER_STATUS_INVALID)).when(orderService).cancel(1L, 8L);

        mockMvc.perform(post("/order/8/cancel").session(loginSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/8"))
                .andExpect(flash().attribute("listMessage", ResultCode.ORDER_STATUS_INVALID.getMessage()));
    }

    // ------------------------------------------------------------ 夹具

    private com.ipc.mall.entity.Order order(Long id, OrderStatus status) {
        com.ipc.mall.entity.Order order = new com.ipc.mall.entity.Order();
        order.setId(id);
        order.setAccountId(1L);
        order.setOrderNo("2026111720150" + id);
        order.setStatus(status.name());
        order.setTotalAmount(new BigDecimal("127.00"));
        order.setFreightAmount(BigDecimal.ZERO);
        order.setCreateTime(LocalDateTime.now().minusMinutes(5));
        return order;
    }

    private CheckoutVO checkoutVO() {
        OrderItemVO item = new OrderItemVO();
        item.setItemId(6L);
        item.setProductId(4L);
        item.setProductName("驾驶员徽章套装（6 枚）");
        item.setSpec("6 枚装");
        item.setPrice(new BigDecimal("39.00"));
        item.setPriceText("39.00");
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("78.00"));
        item.setSubtotalText("78.00");

        CheckoutVO vo = new CheckoutVO();
        vo.setItems(List.of(item));
        vo.setGoodsAmountText("78.00");
        vo.setFreightAmountText("0.00");
        vo.setTotalAmountText("78.00");
        vo.setTotalQuantity(2);
        vo.setCartIds("1,2");
        return vo;
    }

    private MockHttpSession loginSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.LOGIN_ACCOUNT, new LoginAccount(1L, "xingye", "星野"));
        return session;
    }
}
