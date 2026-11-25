package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.CategoryForm;
import com.ipc.mall.entity.Category;
import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.CategoryMapper;
import com.ipc.mall.mapper.ProductMapper;
import com.ipc.mall.service.CategoryAdminService;
import com.ipc.mall.vo.AdminCategoryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
/**
 * 后台分类管理实现（PRD §6.2）。
 * <p>
 * 三条规则落在这里：
 * <ol>
 *     <li>两级结构：上级只能是 0 或某个一级分类；有子分类的一级不能被改成二级；</li>
 *     <li>同名只在同一层级内不允许（数据库没有唯一索引，靠应用层查重）；</li>
 *     <li>删除前先查子分类与商品，命中任一返回 2004。</li>
 * </ol>
 */
@Slf4j
@Service
public class CategoryAdminServiceImpl implements CategoryAdminService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final int VISIBLE = 1;
    private static final int HIDDEN = 0;

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    public CategoryAdminServiceImpl(CategoryMapper categoryMapper, ProductMapper productMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
    }

    // ------------------------------------------------------------------ 读

    @Override
    public List<AdminCategoryVO> tree(String keyword) {
        // 分类总量很小：一次全查后在内存里排序、分组、统计，避免 N+1
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));

        Map<Long, String> rootNames = new HashMap<>();
        Map<Long, List<Category>> childrenByParent = new HashMap<>();
        Map<Long, Integer> productCounts = productCounts();
        for (Category category : all) {
            if (category.isRoot()) {
                rootNames.put(category.getId(), category.getName());
            } else {
                childrenByParent.computeIfAbsent(category.getParentId(), key -> new ArrayList<>()).add(category);
            }
        }

        String needle = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        List<AdminCategoryVO> rows = new ArrayList<>();
        for (Category root : all) {
            if (!root.isRoot()) {
                continue;
            }
            List<Category> children = childrenByParent.getOrDefault(root.getId(), List.of());
            AdminCategoryVO rootVO = AdminCategoryVO.of(root, null,
                    productCounts.getOrDefault(root.getId(), 0), children.size());
            boolean rootMatched = needle == null || matches(root.getName(), needle);
            List<AdminCategoryVO> childVOs = new ArrayList<>();
            for (Category child : children) {
                boolean childMatched = needle == null || matches(child.getName(), needle);
                if (needle == null || rootMatched || childMatched) {
                    childVOs.add(AdminCategoryVO.of(child, root.getName(),
                            productCounts.getOrDefault(child.getId(), 0), 0));
                }
            }
            if (needle == null || rootMatched) {
                rows.add(rootVO);
                // 一级命中时把它下面的二级一起列出来，便于看清结构
                rows.addAll(childVOs);
            } else if (!childVOs.isEmpty()) {
                // 只有二级命中：带上上级（灰色显示为上下文）再列命中的二级
                rows.add(rootVO);
                rows.addAll(childVOs);
            }
        }
        return rows;
    }

    @Override
    public Optional<Category> get(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(categoryMapper.selectById(id));
    }

    @Override
    public List<Category> roots() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, ROOT_PARENT_ID)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    @Override
    public boolean hasChildren(Long id) {
        if (id == null) {
            return false;
        }
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        return count != null && count > 0;
    }

    // ------------------------------------------------------------------ 写

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(CategoryForm form) {
        if (form == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "分类参数缺失");
        }
        String name = trim(form.getName());
        String descn = blankToNull(form.getDescn());
        long parentId = form.getParentId() == null ? ROOT_PARENT_ID : form.getParentId();
        int visible = parseVisible(form.getVisible());
        int sort = form.getSort() == null ? 0 : form.getSort();

        Category existing = form.getId() == null ? null : categoryMapper.selectById(form.getId());
        if (form.getId() != null && existing == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "分类不存在或已被删除");
        }

        if (parentId != ROOT_PARENT_ID) {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null || !parent.isRoot()) {
                throw new BizException(ResultCode.FAIL.getCode(), "上级分类只能是已存在的一级分类");
            }
            if (existing != null && existing.isRoot() && hasChildren(existing.getId())) {
                throw new BizException(ResultCode.FAIL.getCode(), "该分类下还有子分类，不能改为二级分类");
            }
        } else if (existing != null && !existing.isRoot() && hasChildren(existing.getId())) {
            // 二级分类本来就不该有子分类，这里只是防御
            throw new BizException(ResultCode.FAIL.getCode(), "该分类下还有子分类，不能改为一级分类");
        }

        // 同层重名：只用 (parent_id, name) 查，MP 会自动过滤 deleted = 0
        Long sameName = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, parentId)
                .eq(Category::getName, name)
                .ne(form.getId() != null, Category::getId, form.getId()));
        if (sameName != null && sameName > 0) {
            throw new BizException(ResultCode.FAIL.getCode(), "同一层级下已存在同名分类");
        }

        if (existing == null) {
            Category category = new Category();
            category.setParentId(parentId);
            category.setName(name);
            category.setDescn(descn);
            category.setSort(sort);
            category.setVisible(visible);
            categoryMapper.insert(category);
            log.info("新增分类: id={} parent={} name={}", category.getId(), parentId, name);
            return;
        }

        int rows = categoryMapper.updateFields(toCategory(existing.getId(), parentId, name, descn, sort, visible),
                LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(ResultCode.FAIL.getCode(), "分类不存在或已被删除");
        }
        log.info("更新分类: id={} parent={} name={} visible={}", existing.getId(), parentId, name, visible);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Category category = id == null ? null : categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ResultCode.FAIL.getCode(), "分类不存在或已被删除");
        }
        if (hasChildren(id)) {
            throw new BizException(ResultCode.CATEGORY_IN_USE.getCode(), "该分类下还有子分类，无法删除");
        }
        int products = productCount(id);
        if (products > 0) {
            throw new BizException(ResultCode.CATEGORY_IN_USE.getCode(), "该分类下还有 " + products + " 个商品，无法删除");
        }
        // 逻辑删除；两个约束都已经确认过，不做级联
        categoryMapper.deleteById(id);
        log.info("删除分类: id={} name={}", id, category.getName());
    }

    // ------------------------------------------------------------------ 内部方法

    /** 每个分类下的未删除商品数 */
    private Map<Long, Integer> productCounts() {
        Map<Long, Integer> counts = new HashMap<>();
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .isNotNull(Product::getCategoryId));
        for (Product product : products) {
            counts.merge(product.getCategoryId(), 1, Integer::sum);
        }
        return counts;
    }

    private Category toCategory(Long id, long parentId, String name, String descn, int sort, int visible) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setDescn(descn);
        category.setSort(sort);
        category.setVisible(visible);
        return category;
    }

    private boolean matches(String name, String needle) {
        return name != null && name.toLowerCase(Locale.ROOT).contains(needle);
    }

    /** 单个分类下的未删除商品数（删除校验用） */
    private int productCount(Long categoryId) {
        Long count = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, categoryId));
        return count == null ? 0 : count.intValue();
    }

    private int parseVisible(String raw) {
        if (raw == null) {
            return HIDDEN;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "1".equals(value) || "true".equals(value) || "on".equals(value) || "yes".equals(value)
                ? VISIBLE : HIDDEN;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
