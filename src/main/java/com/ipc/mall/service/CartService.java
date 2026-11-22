package com.ipc.mall.service;

import com.ipc.mall.vo.CartItemVO;
import com.ipc.mall.vo.CartVO;

import java.util.List;

/**
 * 购物车（PRD §5.9 / docs/api.md F15–F18）。
 * <p>
 * 所有方法都要求调用方传入<b>会话里的</b> accountId，禁止信任前端传值；
 * 越权（cart 行不属于该账号）统一抛 3004。
 */
public interface CartService {

    /**
     * 加入购物车（F16）：
     * 同 SKU 已存在则累加；已逻辑删除则复活该行；下架 / 库存不足直接拒绝。
     *
     * @param quantity 本次加购数量，1–99
     */
    void add(Long accountId, Long itemId, int quantity);

    /** 修改数量（F17）：1–99 且 ≤ 库存 */
    void updateQuantity(Long accountId, Long cartId, int quantity);

    /** 删除（F18）：逻辑删除 */
    void remove(Long accountId, Long cartId);

    /** 购物车页数据（F15） */
    CartVO list(Long accountId);

    /**
     * 结算用：按 cartIds 取当前账号购物车里的行（顺序不作保证，调用方自行校验）。
     * 只返回确实属于该账号的行，别人的 id 直接忽略。
     */
    List<CartItemVO> listByIds(Long accountId, List<Long> cartIds);

    /** 商品总件数，用于顶部角标；未登录不调用 */
    int count(Long accountId);
}
