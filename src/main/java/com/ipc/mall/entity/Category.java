package com.ipc.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ipc.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类（PRD §7.3 category）。
 * 固定两级：parent_id = 0 表示一级，否则指向某个一级分类的 id。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    /** 0 = 一级分类；否则为一级分类 id */
    private Long parentId;

    /** 分类名，同级内唯一（应用层校验） */
    private String name;

    private String descn;

    /** 升序排列，越小越靠前 */
    private Integer sort;

    /** 1 前台可见 / 0 隐藏 */
    private Integer visible;

    /** 是否一级分类 */
    public boolean isRoot() {
        return parentId == null || parentId == 0L;
    }
}
