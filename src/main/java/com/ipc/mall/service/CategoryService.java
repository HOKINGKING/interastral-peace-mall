package com.ipc.mall.service;

import com.ipc.mall.entity.Category;
import com.ipc.mall.vo.CategoryGroupVO;

import java.util.List;
import java.util.Optional;

/**
 * 前台分类读取（PRD §5.1 / §5.2）。
 * <p>
 * 只暴露读能力：分类的增删改在后台模块，前台不缓存分类树，
 * 保证后台一改、前台刷新即生效。
 */
public interface CategoryService {

    /** 一级分类 + 其下二级分类，按 sort、id 升序，仅 visible=1 */
    List<CategoryGroupVO> visibleGroups();

    /**
     * 全部分类（含隐藏），一级 + 其二级，后台下拉/筛选用。
     * 后台要能管理隐藏分类，所以不能复用 {@link #visibleGroups()}。
     */
    List<CategoryGroupVO> allGroups();

    /** 按 id 查未删除的分类 */
    Optional<Category> find(Long id);

    /** 某个一级分类下可见的二级分类 */
    List<Category> childrenOf(Long rootId);

    /** 分类是否前台可见（visible=1） */
    boolean isVisible(Category category);

    /**
     * 分类筛选范围：
     * 一级分类返回「自身 + 所有二级分类」，二级分类返回自身；分类不存在返回空列表。
     */
    List<Long> selfAndChildIds(Long id);
}
