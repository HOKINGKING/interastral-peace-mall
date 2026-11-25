package com.ipc.mall.vo;

import com.ipc.mall.entity.Category;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 后台分类列表行（PRD §6.2）。
 * <p>
 * 列表按「一级 → 它的二级 → 下一个一级」拍平，页面用 {@code root} 区分缩进与层级标签；
 * {@code productCount} 用来提示删除限制（2004）。
 */
@Data
public class AdminCategoryVO {

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long id;

    private Long parentId;

    /** 二级分类的上级名称；一级为 null */
    private String parentName;

    private boolean root;

    private String name;

    private String descn;

    private Integer sort;

    private boolean visible;

    /** 该分类下的商品数（未删除） */
    private int productCount;

    /** 一级分类下的子分类数 */
    private int childCount;

    private String updateTimeText;

    public static AdminCategoryVO of(Category category, String parentName, int productCount, int childCount) {
        AdminCategoryVO vo = new AdminCategoryVO();
        vo.setId(category.getId());
        vo.setParentId(category.getParentId());
        vo.setParentName(parentName);
        vo.setRoot(category.isRoot());
        vo.setName(category.getName());
        vo.setDescn(category.getDescn());
        vo.setSort(category.getSort());
        vo.setVisible(category.getVisible() != null && category.getVisible() == 1);
        vo.setProductCount(productCount);
        vo.setChildCount(childCount);
        vo.setUpdateTimeText(format(category.getUpdateTime()));
        return vo;
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : MINUTE.format(time);
    }
}
