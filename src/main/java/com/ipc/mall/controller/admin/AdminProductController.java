package com.ipc.mall.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.annotation.OperLog;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.util.PageUtils;
import com.ipc.mall.dto.AdminProductForm;
import com.ipc.mall.dto.SpecPair;
import com.ipc.mall.entity.Category;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Product;
import com.ipc.mall.service.CategoryService;
import com.ipc.mall.service.ProductAdminService;
import com.ipc.mall.vo.AdminProductRowVO;
import com.ipc.mall.vo.CategoryGroupVO;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 后台商品管理（PRD §6.3 / docs/api.md A9–A13）。
 * <p>
 * 列表/表单返回视图；保存用 JSON（SPU + 相册 + 参数表 + SKU 子表一次提交），
 * 上下架与删除返回 {@code Result}。
 */
@Controller
public class AdminProductController {

    private static final int PAGE_SIZE = 10;

    private final ProductAdminService productAdminService;
    private final CategoryService categoryService;

    public AdminProductController(ProductAdminService productAdminService, CategoryService categoryService) {
        this.productAdminService = productAdminService;
        this.categoryService = categoryService;
    }

    /** A9：SPU 列表（名称 / 分类 / 上下架筛选 + 分页） */
    @GetMapping("/admin/product")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false, defaultValue = "1") Integer page,
                       Model model) {
        Integer statusFilter = normalizeStatus(status);
        List<CategoryGroupVO> groups = categoryService.allGroups();
        List<Long> categoryIds = resolveCategoryIds(groups, categoryId);
        IPage<AdminProductRowVO> products = productAdminService.page(keyword, categoryIds, statusFilter, page, PAGE_SIZE);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", keyword);
        params.put("categoryId", categoryId);
        params.put("status", statusFilter);
        model.addAttribute("pageTitle", "商品管理");
        model.addAttribute("products", products.getRecords());
        model.addAttribute("total", products.getTotal());
        model.addAttribute("pagination", PageUtils.pagination(products, "/admin/product", params));
        model.addAttribute("groups", groups);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", statusFilter);
        return "admin/product/list";
    }

    /** A10：新增 / 编辑表单（SPU 基本信息 + 相册 + 参数表 + SKU 子表） */
    @GetMapping({"/admin/product/form", "/admin/product/form/{id}"})
    public String form(@PathVariable(value = "id", required = false) Long id,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        Product product = null;
        List<Item> items = List.of();
        List<SpecPair> specs = List.of();
        List<String> gallery = List.of();
        if (id != null) {
            Optional<Product> found = productAdminService.get(id);
            if (found.isEmpty()) {
                redirectAttributes.addFlashAttribute("listMessage", "商品不存在或已被删除");
                return "redirect:/admin/product";
            }
            product = found.get();
            items = productAdminService.itemsOf(id);
            specs = productAdminService.specPairsOf(product);
            gallery = productAdminService.galleryOf(product);
        }
        model.addAttribute("pageTitle", id == null ? "新增商品" : "编辑商品");
        model.addAttribute("product", product);
        model.addAttribute("items", items);
        model.addAttribute("specs", specs);
        model.addAttribute("gallery", gallery);
        model.addAttribute("groups", categoryService.allGroups());
        return "admin/product/form";
    }

    /** A11：保存 SPU + SKU（JSON 请求体） */
    @PostMapping("/admin/product/save")
    @OperLog(module = "商品管理", action = "保存商品")
    @ResponseBody
    public Result<Void> save(@Valid @RequestBody AdminProductForm form) {
        boolean creating = form.getId() == null;
        productAdminService.save(form);
        return Result.ok(creating ? "商品已创建" : "商品已保存", (Void) null);
    }

    /** A12：上下架 */
    @PostMapping("/admin/product/{id}/status")
    @OperLog(module = "商品管理", action = "商品上下架")
    @ResponseBody
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam(required = false) Integer status) {
        productAdminService.updateStatus(id, status);
        return Result.ok(status != null && status == 1 ? "商品已上架" : "商品已下架", (Void) null);
    }

    /** A13：删除（有未完结订单返回 2005） */
    @DeleteMapping("/admin/product/{id}")
    @OperLog(module = "商品管理", action = "删除商品")
    @ResponseBody
    public Result<Void> remove(@PathVariable Long id) {
        productAdminService.remove(id);
        return Result.ok("商品已删除", (Void) null);
    }

    // ------------------------------------------------------------------ 内部方法

    /** 一级分类 → 「自身 + 全部子分类」；二级 → 自身；找不到 → 空列表（返回空结果） */
    private List<Long> resolveCategoryIds(List<CategoryGroupVO> groups, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        for (CategoryGroupVO group : groups) {
            if (group.getRoot().getId().equals(categoryId)) {
                List<Long> ids = new ArrayList<>();
                ids.add(categoryId);
                for (Category child : group.getChildren()) {
                    ids.add(child.getId());
                }
                return ids;
            }
            for (Category child : group.getChildren()) {
                if (child.getId().equals(categoryId)) {
                    return List.of(categoryId);
                }
            }
        }
        return List.of();
    }

    private Integer normalizeStatus(Integer status) {
        return status != null && (status == 0 || status == 1) ? status : null;
    }
}
