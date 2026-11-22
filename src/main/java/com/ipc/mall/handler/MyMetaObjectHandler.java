package com.ipc.mall.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充（PRD §7.1）。
 * <p>
 * 填充的字段名必须与 {@link com.ipc.mall.common.entity.BaseEntity} 的属性名一致：
 * <b>createTime / updateTime / deleted</b>。
 * <p>
 * deleted 必须在 insert 时显式填 0：数据库列虽然是 {@code DEFAULT 0}，
 * 但 MyBatis-Plus 会把实体里的 null 当成 SQL 的 NULL 插进去，触发 NOT NULL 约束错误。
 * <p>
 * 注意 {@code order_item} 这类表<b>不继承 BaseEntity</b>，没有 update_time / deleted，
 * 所以每个字段都要先判断实体上是否存在，否则反射取值会抛
 * {@code There is no getter for property named 'deleted'}。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        if (metaObject.hasGetter("deleted") && metaObject.getValue("deleted") == null) {
            this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
