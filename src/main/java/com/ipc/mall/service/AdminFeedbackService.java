package com.ipc.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.vo.AdminFeedbackRowVO;
import com.ipc.mall.vo.FeedbackDetailVO;

import java.time.LocalDate;

/**
 * 后台投诉与反馈管理（PRD §6.6 / docs/api.md A28–A32）。
 * <p>
 * 单一角色，不做行级隔离；受理/回复/关闭都由 Controller 记操作日志（模块「售后管理」）。
 */
public interface AdminFeedbackService {

    /** A28：按类型/状态/订单号/用户名/时间区间分页，PENDING 优先 */
    IPage<AdminFeedbackRowVO> page(String type, String status, String orderNo, String username,
                                   LocalDate dateFrom, LocalDate dateTo, int pageNo, int size);

    /** A29：详情（内容、补充说明、关联订单摘要） */
    FeedbackDetailVO detail(Long id);

    /** A30：受理 PENDING → PROCESSING，重复受理 3002 */
    void accept(Long id);

    /** A31：回复（PENDING/PROCESSING），写回复人、内容与时间 → REPLIED */
    void reply(Long id, String reply, Long adminId);

    /** A32：关闭（未关闭状态均可），写关闭原因 → CLOSED */
    void close(Long id, String closeReason);
}
