package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.SysOperLog;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作日志 Mapper：AOP 切面只 insert，后台日志页按 module / 时间范围分页查询。
 * 该表不做逻辑删除，BaseMapper 的查询不会追加 deleted 条件。
 */
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {

    /** 筛选项：库里出现过的模块名（去重排序），给后台日志页的下拉框用 */
    @Select("SELECT DISTINCT module FROM sys_oper_log WHERE module IS NOT NULL AND module <> '' ORDER BY module")
    List<String> selectDistinctModules();
}
