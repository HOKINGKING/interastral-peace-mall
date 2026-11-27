package com.ipc.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.dto.FeedbackForm;
import com.ipc.mall.vo.FeedbackDetailVO;
import com.ipc.mall.vo.FeedbackFormVO;
import com.ipc.mall.vo.FeedbackRowVO;

/**
 * 前台投诉与反馈（PRD §5.15 / docs/api.md F32–F39）。
 * <p>
 * 所有按 id 的操作都以会话里的 accountId 做归属校验，禁止信任前端传的账号信息。
 */
public interface FeedbackService {

    /** F34：我的投诉/反馈分页，支持类型与状态筛选 */
    IPage<FeedbackRowVO> pageMine(Long accountId, String type, String status, int pageNo, int size);

    /** F35：详情（非本人按不存在处理） */
    FeedbackDetailVO detail(Long accountId, Long id);

    /** F32：表单上下文（投诉时带订单快照，已有未关闭投诉则标记 blocked） */
    FeedbackFormVO formContext(Long accountId, Long orderId);

    /** F33：提交（orderId 有值即投诉），校验订单归属/状态/重复与反馈频率 */
    void submit(Long accountId, FeedbackForm form);

    /** F36：撤销，仅 PENDING 可撤销 */
    void cancel(Long accountId, Long id);

    /** F38：追加补充说明，不改变状态，CLOSED 后不可追加 */
    void addNote(Long accountId, Long id, String note);

    /** F39：用户确认已解决，仅 REPLIED → CLOSED */
    void resolve(Long accountId, Long id);
}
