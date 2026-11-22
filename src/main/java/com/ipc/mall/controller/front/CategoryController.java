package com.ipc.mall.controller.front;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.common.util.PageUtils;
import com.ipc.mall.dto.ProductQuery;
import com.ipc.mall.entity.Category;
import com.ipc.mall.service.CategoryService;
import com.ipc.mall.service.ProductService;
import com.ipc.mall.vo.ProductCardVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分类落地页（PRD §5.2 / §10.2 F3）。
 * <ul>
 *     <li>一级分类：顶部列出全部二级分类入口，商品覆盖所有子分类</li>
 *     <li>二级分类：同级切换 + 当前分类商品，面包屑「首页 › 一级 › 二级」</li>
 *     <li>分类不存在 → 404；visible=0 → 302 到列表页并提示</li>
 * </ul>
 */
@Controller
public class CategoryController {

    private static final int SIZE_DEFAULT = 12;

    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping("/category/{id}")
    public String category(@PathVariable Long id,
                           @ModelAttribute("query") ProductQuery query,
                           Model model,
                           HttpServletResponse response,
                           RedirectAttributes redirectAttributes) {
        Optional<Category> found = categoryService.find(id);
        if (found.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "error/404";
        }
        Category category = found.get();
        if (!categoryService.isVisible(category)) {
            redirectAttributes.addFlashAttribute("listMessage", "该分类已下架，已为你展示全部商品");
            return "redirect:/product/list";
        }

        boolean root = category.getParentId() == null || category.getParentId() == 0L;
        Category parent = root ? null : categoryService.find(category.getParentId()).orElse(null);

        query.setCategoryId(category.getId());
        query.normalize();
        IPage<ProductCardVO> page = productService.pageCards(query,
                categoryService.selfAndChildIds(category.getId()));

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("sort", ProductQuery.SORT_DEFAULT.equals(query.getSort()) ? null : query.getSort());
        params.put("size", query.getSize() == SIZE_DEFAULT ? null : query.getSize());
        String basePath = "/category/" + category.getId();

        model.addAttribute("category", category);
        model.addAttribute("parentCategory", parent);
        model.addAttribute("isRoot", root);
        model.addAttribute("rootCategory", root ? category : parent);
        model.addAttribute("activeNav", "category-" + category.getId());
        model.addAttribute("chips", categoryService.childrenOf(root ? category.getId() : category.getParentId()));
        model.addAttribute("page", page);
        model.addAttribute("pagination", PageUtils.pagination(page, basePath, params));
        model.addAttribute("sort", query.getSort());
        model.addAttribute("size", query.getSize());
        model.addAttribute("pageTitle", category.getName());
        return "front/category/index";
    }
}
