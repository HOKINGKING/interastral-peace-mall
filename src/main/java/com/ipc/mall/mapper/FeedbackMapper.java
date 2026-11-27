package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.entity.Feedback;
import com.ipc.mall.vo.AdminFeedbackRowVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 投诉与反馈 Mapper（PRD §5.15 / §6.6 / §8.4）。
 * <p>
 * 所有状态流转都用<b>条件更新</b>：影响行数为 0 一律返回 3002，
 * 既做并发兜底，也保证「重复受理 / 重复关闭」不会覆盖已有结果。
 */
public interface FeedbackMapper extends BaseMapper<Feedback> {

    /** A28：后台列表，PENDING 优先、其次按提交时间倒序 */
    IPage<AdminFeedbackRowVO> selectAdminPage(IPage<AdminFeedbackRowVO> page,
                                             @Param("type") String type,
                                             @Param("status") String status,
                                             @Param("orderNo") String orderNo,
                                             @Param("username") String username,
                                             @Param("dateFrom") LocalDateTime dateFrom,
                                             @Param("dateTo") LocalDateTime dateTo);

    /** 同一订单是否已有未关闭的投诉（PENDING / PROCESSING / REPLIED） */
    @Select("SELECT COUNT(*) FROM feedback WHERE type = 'COMPLAINT' AND order_id = #{orderId} "
            + "AND account_id = #{accountId} AND status <> 'CLOSED' AND deleted = 0")
    long countOpenComplaint(@Param("orderId") Long orderId, @Param("accountId") Long accountId);

    /** 该用户自 since 起提交的反馈条数（1 小时 ≤5 条） */
    @Select("SELECT COUNT(*) FROM feedback WHERE type = 'FEEDBACK' AND account_id = #{accountId} "
            + "AND create_time >= #{since} AND deleted = 0")
    long countRecentFeedback(@Param("accountId") Long accountId, @Param("since") LocalDateTime since);

    /** C2 受理：PENDING → PROCESSING */
    @Update("UPDATE feedback SET status = 'PROCESSING', update_time = #{updateTime} "
            + "WHERE id = #{id} AND status = 'PENDING' AND deleted = 0")
    int markAccepted(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    /** C3 回复：PENDING / PROCESSING → REPLIED，写回复人与回复时间 */
    @Update("UPDATE feedback SET status = 'REPLIED', reply = #{reply}, reply_admin_id = #{adminId}, "
            + "reply_time = #{replyTime}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND status IN ('PENDING', 'PROCESSING') AND deleted = 0")
    int markReplied(@Param("id") Long id,
                    @Param("reply") String reply,
                    @Param("adminId") Long adminId,
                    @Param("replyTime") LocalDateTime replyTime,
                    @Param("updateTime") LocalDateTime updateTime);

    /** C5 卖家关闭：未关闭 → CLOSED */
    @Update("UPDATE feedback SET status = 'CLOSED', close_reason = #{closeReason}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND status <> 'CLOSED' AND deleted = 0")
    int markClosedByAdmin(@Param("id") Long id,
                          @Param("closeReason") String closeReason,
                          @Param("updateTime") LocalDateTime updateTime);

    /** C4 用户撤销：仅 PENDING → CLOSED，原因固定 */
    @Update("UPDATE feedback SET status = 'CLOSED', close_reason = '用户撤销', update_time = #{updateTime} "
            + "WHERE id = #{id} AND account_id = #{accountId} AND status = 'PENDING' AND deleted = 0")
    int cancelByUser(@Param("id") Long id,
                     @Param("accountId") Long accountId,
                     @Param("updateTime") LocalDateTime updateTime);

    /** C6 用户确认已解决：仅 REPLIED → CLOSED，原因固定 */
    @Update("UPDATE feedback SET status = 'CLOSED', close_reason = '用户确认已解决', update_time = #{updateTime} "
            + "WHERE id = #{id} AND account_id = #{accountId} AND status = 'REPLIED' AND deleted = 0")
    int resolveByUser(@Param("id") Long id,
                      @Param("accountId") Long accountId,
                      @Param("updateTime") LocalDateTime updateTime);

    /** 补充说明：不改变状态，CLOSED 后不允许追加 */
    @Update("UPDATE feedback SET user_note = #{userNote}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND account_id = #{accountId} AND status <> 'CLOSED' AND deleted = 0")
    int updateUserNote(@Param("id") Long id,
                       @Param("accountId") Long accountId,
                       @Param("userNote") String userNote,
                       @Param("updateTime") LocalDateTime updateTime);
}
