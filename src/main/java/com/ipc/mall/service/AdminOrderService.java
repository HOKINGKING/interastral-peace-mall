package com.ipc.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.dto.ReceiverForm;
import com.ipc.mall.dto.ShipForm;
import com.ipc.mall.vo.AdminOrderRowVO;
import com.ipc.mall.vo.OrderDetailVO;

import java.time.LocalDate;

/**
 * 后台订单管理（PRD §6.5 / docs/api.md A22–A26）。
 * <p>
 * 发货走状态机条件更新（PAID → SHIPPED）；删除只允许终态且不回补库存；
 * 后台只有一种角色，不做行级归属校验，但所有变更都会记操作日志（A27 后续接入）。
 */
public interface AdminOrderService {

    /** A22：分页列表，支持订单号 / 用户名 / 状态 / 下单时间区间 */
    IPage<AdminOrderRowVO> page(String orderNo, String username, String status,
                                LocalDate dateFrom, LocalDate dateTo, int pageNo, int size);

    /** A23：详情（含下单用户名、明细、时间线、物流） */
    OrderDetailVO detail(Long id);

    /** A24：发货（仅 PAID，重复发货 3002） */
    void ship(Long id, ShipForm form);

    /** A25：修改收货信息与备注（金额/明细锁定） */
    void updateReceiver(Long id, ReceiverForm form);

    /** A26：删除（仅 CANCELLED / COMPLETED，逻辑删除，不回补库存） */
    void remove(Long id);
}
