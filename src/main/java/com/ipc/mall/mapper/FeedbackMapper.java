package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.Feedback;

/**
 * 投诉与反馈 Mapper。
 * <p>
 * 状态流转同样用条件更新（PRD §8.4）：{@code WHERE id = ? AND status = ?}，
 * 影响行数 0 返回 3002。受理/回复/关闭各自只允许从特定状态进入。
 */
public interface FeedbackMapper extends BaseMapper<Feedback> {
}
