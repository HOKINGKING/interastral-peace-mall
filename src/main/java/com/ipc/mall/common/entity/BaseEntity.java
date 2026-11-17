package com.ipc.mall.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务实体基类：只放三张八表共有的列，其余字段由子类声明。
 * <p>
 * 对应数据库列（注意是 created_at / updated_at，不是 create_time）：
 * <ul>
 *     <li>id          BIGINT AUTO_INCREMENT</li>
 *     <li>created_at  DATETIME，插入时自动填充</li>
 *     <li>updated_at  DATETIME，插入与更新时自动填充</li>
 * </ul>
 * <p>
 * 这里刻意<b>没有 deleted 字段</b>：本项目不使用逻辑删除，
 * 商品用 status 上下架、订单用 status 取消，由 CHECK 与外键约束兜底。
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，与表的 AUTO_INCREMENT 对应 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 创建时间，由 MyMetaObjectHandler 在 insert 时填充 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间，由 MyMetaObjectHandler 在 insert / update 时填充 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
