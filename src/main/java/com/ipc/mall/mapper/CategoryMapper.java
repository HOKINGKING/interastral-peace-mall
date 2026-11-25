package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.Category;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/** 分类 Mapper：后台的增删改由 Service 组装，这里只补充整行覆盖的更新 */
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 后台编辑分类：整行覆盖（描述允许被清空，所以用显式 set 而不是 updateById 跳过 null）。
     * 影响行数 0 说明分类已被删除。
     */
    @Update("UPDATE category SET parent_id = #{c.parentId}, name = #{c.name}, descn = #{c.descn}, "
            + "sort = #{c.sort}, visible = #{c.visible}, update_time = #{updateTime} "
            + "WHERE id = #{c.id} AND deleted = 0")
    int updateFields(@Param("c") Category category, @Param("updateTime") LocalDateTime updateTime);
}
