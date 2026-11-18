package com.ipc.mall.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务实体基类，对应 PRD §7.1 的公共字段约定。
 * <ul>
 *     <li>id          BIGINT AUTO_INCREMENT，MP {@code id-type: auto}</li>
 *     <li>create_time 插入时自动填充</li>
 *     <li>update_time 插入与更新时自动填充</li>
 *     <li>deleted     逻辑删除，0 正常 / 1 已删除，查询时 MP 自动追加 {@code deleted = 0}</li>
 * </ul>
 * 注意：order_item 与 sys_oper_log 没有 update_time / deleted，
 * 这两个实体<b>不要继承本类</b>，各自声明字段。
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记。查询自动带 deleted = 0，deleteById 会改成 UPDATE deleted = 1。
     * 唯一索引与逻辑删除的坑见 PRD §7.5：account/admin 的唯一键建在 (username, deleted)。
     */
    @TableLogic(value = "0", delval = "1")
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Integer deleted;
}
