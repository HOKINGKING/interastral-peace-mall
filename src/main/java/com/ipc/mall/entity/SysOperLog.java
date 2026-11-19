package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志（PRD §7.3 sys_oper_log），由 {@code @OperLog} 注解 + AOP 切面写入。
 * <p>
 * <b>不继承 BaseEntity</b>：日志只追加不修改，所以没有 update_time，也不做逻辑删除。
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 后台操作时是 admin.id；前台登录类日志记 0 */
    private Long adminId;

    /** 操作人名称，admin 不可用时记录用户名 */
    private String adminName;

    /** 模块，如「订单管理」 */
    private String module;

    /** 动作，如「发货」 */
    private String action;

    /** 目标主键，便于按单号/商品反查 */
    private Long targetId;

    private String requestUri;

    private String requestMethod;

    /** 入参 JSON，password 等敏感字段必须脱敏后再写入 */
    private String requestParams;

    /** 客户端 IP，VARCHAR(45) 兼容 IPv6 */
    private String ip;

    /** 1 成功 / 0 失败 */
    private Integer success;

    /** 失败原因 */
    private String errorMsg;

    /** 耗时毫秒 */
    private Integer costMs;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
