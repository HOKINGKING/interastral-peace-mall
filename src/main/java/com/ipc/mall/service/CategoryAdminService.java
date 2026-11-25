package com.ipc.mall.service;

import com.ipc.mall.dto.CategoryForm;
import com.ipc.mall.entity.Category;
import com.ipc.mall.vo.AdminCategoryVO;

import java.util.List;
import java.util.Optional;

/**
 * 后台分类管理（PRD §6.2 / docs/api.md A5–A8）。
 * <p>
 * 两级结构：{@code parent_id=0} 为一级，其余为二级。
 * 删除只要碰到「有子分类」或「有商品」就返回 2004，不做级联删除。
 */
public interface CategoryAdminService {

    /**
     * 列表：一级分类与其二级分类按 sort、id 顺序拍平。
     *
     * @param keyword 为空返回全部；否则按名称包含匹配
     */
    List<AdminCategoryVO> tree(String keyword);

    /** 表单回填 */
    Optional<Category> get(Long id);

    /** 一级分类列表（表单里的「上级分类」下拉） */
    List<Category> roots();

    /** 该分类下是否还有未删除的子分类 */
    boolean hasChildren(Long id);

    /** 新增 / 编辑；校验同层重名、上级必须是一级、有子分类的一级不能降级为二级 */
    void save(CategoryForm form);

    /** 逻辑删除；有子分类或商品时抛 2004 */
    void remove(Long id);
}
