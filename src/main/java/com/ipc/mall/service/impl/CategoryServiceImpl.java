package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ipc.mall.entity.Category;
import com.ipc.mall.mapper.CategoryMapper;
import com.ipc.mall.service.CategoryService;
import com.ipc.mall.vo.CategoryGroupVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分类读取实现（PRD §5.1 / §5.2）。
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private static final int VISIBLE = 1;
    private static final long ROOT_PARENT_ID = 0L;

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryGroupVO> visibleGroups() {
        // 分类总量很小，一次查出后在内存分组，避免「一级一次 + 每个一级再查一次」的 N+1
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getVisible, VISIBLE)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));

        List<Category> roots = new ArrayList<>();
        Map<Long, List<Category>> childrenByParent = new HashMap<>();
        for (Category category : all) {
            if (isRoot(category)) {
                roots.add(category);
            } else {
                childrenByParent
                        .computeIfAbsent(category.getParentId(), key -> new ArrayList<>())
                        .add(category);
            }
        }

        return roots.stream()
                .map(root -> new CategoryGroupVO(root, childrenByParent.getOrDefault(root.getId(), List.of())))
                .toList();
    }

    @Override
    public Optional<Category> find(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(categoryMapper.selectById(id));
    }

    @Override
    public List<Category> childrenOf(Long rootId) {
        if (rootId == null) {
            return List.of();
        }
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, rootId)
                .eq(Category::getVisible, VISIBLE)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    @Override
    public boolean isVisible(Category category) {
        return category != null && category.getVisible() != null && category.getVisible() == VISIBLE;
    }

    @Override
    public List<Long> selfAndChildIds(Long id) {
        Optional<Category> found = find(id);
        if (found.isEmpty()) {
            return List.of();
        }
        Category category = found.get();
        List<Long> ids = new ArrayList<>();
        ids.add(category.getId());
        if (isRoot(category)) {
            childrenOf(category.getId()).forEach(child -> ids.add(child.getId()));
        }
        return ids;
    }

    private boolean isRoot(Category category) {
        return category.getParentId() == null || category.getParentId() == ROOT_PARENT_ID;
    }
}
