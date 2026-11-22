package com.ipc.mall.controller.front;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.common.util.FilterLinkUtils;
import com.ipc.mall.common.util.PageUtils;
import com.ipc.mall.dto.ProductQuery;
import com.ipc.mall.entity.Category;
import com.ipc.mall.service.CategoryService;
import com.ipc.mall.service.ProductService;
import com.ipc.mall.vo.ProductCardVO;
import com.ipc.mall.vo.ProductDetailVO;
import com.ipc.mall.vo.SkuVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 前台商品页（PRD §5.3 / §5.4 / §10.2 F2–F6）：
 * 列表、搜索、详情、SKU 局部刷新。
 */
@Controller
public class ProductController {

    private static final int SIZE_DEFAULT = 12;

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/product/list")
    public String list(@ModelAttribute("query") ProductQuery query, Model model) {
        return renderList(query, model, "/product/list", "全部商品");
    }

    /** 搜索与列表共用视图与筛选逻辑（PRD §5.3） */
    @GetMapping("/search")
    public String search(@ModelAttribute("query") ProductQuery query, Model model) {
        query.normalize();
        if (!query.hasKeyword()) {
            return "redirect:/product/list";
        }
        return renderList(query, model, "/search", "搜索");
    }

    @GetMapping("/product/{id}")
    public String detail(@PathVariable Long id, Model model, HttpServletResponse response) {
        Optional<ProductDetailVO> detail = productService.detail(id);
        if (detail.isEmpty()) {
            // 商品不存在（含已逻辑删除）：保持 404 语义，渲染统一错误页
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "error/404";
        }
        ProductDetailVO detailVO = detail.get();
        model.addAttribute("detail", detailVO);
        Category navCategory = detailVO.getParentCategory() != null
                ? detailVO.getParentCategory()
                : detailVO.getCategory();
        model.addAttribute("activeNav", navCategory != null ? "category-" + navCategory.getId() : "list");
        return "front/product/detail";
    }

    /** SKU 局部刷新（PRD §5.4）：切换规格时只换价格/库存/按钮状态，不整页刷新 */
    @GetMapping("/product/{id}/skus")
    @ResponseBody
    public Result<List<SkuVO>> skus(@PathVariable Long id) {
        if (!productService.exists(id)) {
            return Result.fail(ResultCode.NOT_FOUND);
        }
        return Result.ok(productService.skusOf(id));
    }

    // ------------------------------------------------------------------ 内部方法

    private String renderList(ProductQuery query, Model model, String basePath, String pageTitle) {
        query.normalize();
        List<Long> categoryIds = query.getCategoryId() == null
                ? null
                : categoryService.selfAndChildIds(query.getCategoryId());
        IPage<ProductCardVO> page = productService.pageCards(query, categoryIds);
        Map<String, Object> params = filterParams(query);

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("basePath", basePath);
        model.addAttribute("page", page);
        model.addAttribute("pagination", PageUtils.pagination(page, basePath, params));
        model.addAttribute("sortLinks", FilterLinkUtils.sortLinks(basePath, params, query.getSort()));
        model.addAttribute("sidebarGroups", categoryService.visibleGroups());
        model.addAttribute("selectedCategoryName", categoryName(query.getCategoryId()));
        model.addAttribute("keywordChipUrl", query.hasKeyword()
                ? PageUtils.filterUrl(basePath, PageUtils.without(params, "keyword"))
                : null);
        model.addAttribute("categoryChipUrl", query.getCategoryId() == null
                ? null
                : PageUtils.filterUrl(basePath, PageUtils.without(params, "categoryId")));
        model.addAttribute("clearUrl", PageUtils.filterUrl(basePath, Map.of()));
        return "front/product/list";
    }

    /** 只把有值的筛选项放进 URL，避免 ?sort=default&amp;size=12 这类噪音 */
    private Map<String, Object> filterParams(ProductQuery query) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("categoryId", query.getCategoryId());
        params.put("keyword", query.getKeyword());
        params.put("onlyInStock", query.isOnlyInStock());
        params.put("sort", ProductQuery.SORT_DEFAULT.equals(query.getSort()) ? null : query.getSort());
        params.put("size", query.getSize() == SIZE_DEFAULT ? null : query.getSize());
        return params;
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryService.find(categoryId).map(Category::getName).orElse(null);
    }
}
