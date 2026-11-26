package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.ReceiverForm;
import com.ipc.mall.dto.ShipForm;
import com.ipc.mall.entity.Account;
import com.ipc.mall.entity.Order;
import com.ipc.mall.entity.OrderItem;
import com.ipc.mall.mapper.AccountMapper;
import com.ipc.mall.mapper.OrderItemMapper;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.service.AdminOrderService;
import com.ipc.mall.vo.AdminOrderRowVO;
import com.ipc.mall.vo.OrderDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台订单管理实现（PRD §6.5）。
 * <p>
 * 三条规则：
 * <ol>
 *     <li>发货只允许 PAID → SHIPPED，条件更新 0 行即 3002（重复发货也被挡住）；</li>
 *     <li>改收货信息只动快照列与备注，金额/明细永不改；</li>
 *     <li>删除只允许终态，逻辑删除且<b>不回补库存</b>（取消时已补过）。</li>
 * </ol>
 */
@Slf4j
@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final String NOT_FOUND = "订单不存在";

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final AccountMapper accountMapper;

    public AdminOrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper, AccountMapper accountMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.accountMapper = accountMapper;
    }

    // ------------------------------------------------------------------ 列表 / 详情

    @Override
    public IPage<AdminOrderRowVO> page(String orderNo, String username, String status,
                                      LocalDate dateFrom, LocalDate dateTo, int pageNo, int size) {
        Page<AdminOrderRowVO> page = new Page<>(Math.max(1, pageNo), Math.max(1, size));
        // 结束日期按「次日 00:00」做开区间，含结束当天全天
        LocalDateTime from = dateFrom == null ? null : dateFrom.atStartOfDay();
        LocalDateTime to = dateTo == null ? null : dateTo.plusDays(1).atStartOfDay();
        return orderMapper.selectAdminPage(page, trimToNull(orderNo), trimToNull(username),
                normalizeStatus(status), from, to);
    }

    @Override
    public OrderDetailVO detail(Long id) {
        Order order = requireOrder(id);
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, id));
        Account account = order.getAccountId() == null ? null : accountMapper.selectById(order.getAccountId());
        String username = account == null ? null : account.getUsername();
        return OrderDetailVO.of(order, items, username);
    }

    // ------------------------------------------------------------------ 写操作

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Long id, ShipForm form) {
        Order order = requireOrder(id);
        if (!OrderStatus.of(order.getStatus()).canTransitTo(OrderStatus.SHIPPED)) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        String courier = trimToNull(form.getCourierCompany());
        String tracking = trimToNull(form.getTrackingNo());
        if (courier == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "请输入物流公司");
        }
        if (tracking == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "请输入物流单号");
        }
        if (courier.length() > 30) {
            throw new BizException(ResultCode.FAIL.getCode(), "物流公司最多 30 个字符");
        }
        if (tracking.length() > 40) {
            throw new BizException(ResultCode.FAIL.getCode(), "物流单号最多 40 个字符");
        }
        LocalDateTime now = LocalDateTime.now();
        // 条件更新：PAID → SHIPPED 只成功一次，重复发货 rows=0 即 3002
        int rows = orderMapper.markShipped(id, courier, tracking, now, now);
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        log.info("后台发货: orderId={} courier={} tracking={}", id, courier, tracking);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReceiver(Long id, ReceiverForm form) {
        Order order = requireOrder(id);
        String name = trimToNull(form.getReceiverName());
        String phone = trimToNull(form.getReceiverPhone());
        String detail = trimToNull(form.getReceiverDetail());
        if (name == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "请输入收货人");
        }
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            throw new BizException(ResultCode.FAIL.getCode(), "手机号需为 11 位数字");
        }
        if (detail == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "请输入详细地址");
        }
        if (form.getRemark() != null && form.getRemark().trim().length() > 200) {
            throw new BizException(ResultCode.FAIL.getCode(), "备注最多 200 字");
        }
        // remark 为 null 表示不改（表单没带这个字段），空串表示清空
        String remark = form.getRemark() == null ? order.getRemark() : trimToNull(form.getRemark());
        int rows = orderMapper.updateReceiver(id, name, phone, detail, remark, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.FAIL.getCode(), NOT_FOUND);
        }
        log.info("后台修改收货信息: orderId={} receiver={}", id, name);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Order order = requireOrder(id);
        if (!OrderStatus.of(order.getStatus()).isFinal()) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        // 逻辑删除，不碰库存（已取消订单取消时已回补过）
        orderMapper.deleteById(id);
        log.info("后台删除订单: orderId={} no={}", id, order.getOrderNo());
    }

    // ------------------------------------------------------------------ 内部方法

    private Order requireOrder(Long id) {
        Order order = id == null ? null : orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND.getCode(), NOT_FOUND);
        }
        return order;
    }

    /** 状态筛选：只接受合法枚举名，空/非法按「全部」 */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String value = status.trim();
        for (OrderStatus candidate : OrderStatus.values()) {
            if (candidate.name().equals(value)) {
                return value;
            }
        }
        log.warn("忽略非法的订单状态筛选: {}", value);
        return null;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
