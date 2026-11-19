package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.SysOperLog;

/**
 * 操作日志 Mapper：AOP 切面只 insert，后台日志页按 module / 时间范围分页查询。
 * 该表不做逻辑删除，BaseMapper 的查询不会追加 deleted 条件。
 */
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
}
