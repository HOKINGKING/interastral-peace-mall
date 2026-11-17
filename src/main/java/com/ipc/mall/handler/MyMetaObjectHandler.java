package com.ipc.mall.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充器（IoC 体现点：由 Spring 注入到 MyBatis-Plus 的全局配置中）。
 * <p>
 * 填充字段名必须与 {@link com.ipc.mall.common.entity.BaseEntity} 的属性名一致：
 * <b>createdAt / updatedAt</b>。
 * <p>
 * strictInsertFill / strictUpdateFill 的行为：
 * <ul>
 *     <li>strict* 系列会先判断当前属性是否有 setter、是否已经赋过值；</li>
 *     <li>已手动赋值时不再覆盖，避免把业务代码显式设置的时间冲掉。</li>
 * </ul>
 * 注意：填充是"Java 侧兜底"，数据库列本身还有 DEFAULT CURRENT_TIMESTAMP，
 * 所以即使填充失效也不会报错——验收时必须看控制台 SQL 里是否带上了这两列。
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
