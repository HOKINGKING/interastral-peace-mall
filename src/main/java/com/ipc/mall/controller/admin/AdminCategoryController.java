package com.ipc.mall.controller.admin;

import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.CategoryForm;
import com.ipc.mall.entity.Category;
import com.ipc.mall.service.CategoryAdminService;
import com.ipc.mall.vo.AdminCategoryVO;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * 后台分类管理（PRD §6.2 / docs/api.md A5–A8）。
 * <p>
 * 列表与表单返回视图，保存/删除返回 {@code Result}（页面用 fetch 提交，与前台模块一致）。
 * 登录态由 {@code AdminLoginInterceptor} 统一拦截，这里不再重复判断。
 */
@Controller
public class AdminCategoryController {

    private final CategoryAdminService categoryAdminService;

    public AdminCategoryController(CategoryAdminService categoryAdminService) {
        this.categoryAdminService = categoryAdminService;
    }

    /** A5：分类列表（两级，按名称搜索） */
    @GetMapping("/admin/category")
    public String list(@RequestParam(required = false) String keyword, Model model) {
        List<AdminCategoryVO> categories = categoryAdminService.tree(keyword);
        model.addAttribute("pageTitle", "分类管理");
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", keyword);
        return "admin/category/list";
    }

    /** A6：新增 / 编辑表单 */
    @GetMapping({"/admin/category/form", "/admin/category/form/{id}"})
    public String form(@PathVariable(value = "id", required = false) Long id,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        CategoryForm form = new CategoryForm();
        boolean lockedParent = false;
        if (id != null) {
            Optional<Category> found = categoryAdminService.get(id);
            if (found.isEmpty()) {
                redirectAttributes.addFlashAttribute("listMessage", "分类不存在或已被删除");
                return "redirect:/admin/category";
            }
            Category category = found.get();
            fill(form, category);
            // 一级分类下面还有子分类时，不允许把它改成二级
            lockedParent = category.isRoot() && categoryAdminService.hasChildren(category.getId());
        }
        model.addAttribute("pageTitle", id == null ? "新增分类" : "编辑分类");
        model.addAttribute("categoryForm", form);
        model.addAttribute("roots", categoryAdminService.roots());
        model.addAttribute("lockedParent", lockedParent);
        return "admin/category/form";
    }

    /** A7：保存 */
    @PostMapping("/admin/category/save")
    @ResponseBody
    public Result<Void> save(@Valid @ModelAttribute("categoryForm") CategoryForm categoryForm,
                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getAllErrors().isEmpty()
                    ? ResultCode.FAIL.getMessage()
                    : bindingResult.getAllErrors().get(0).getDefaultMessage();
            return Result.fail(ResultCode.FAIL.getCode(), message);
        }
        boolean creating = categoryForm.getId() == null;
        categoryAdminService.save(categoryForm);
        return Result.ok(creating ? "分类已创建" : "分类已更新", (Void) null);
    }

    /** A8：删除（有子分类或商品时 2004） */
    @DeleteMapping("/admin/category/{id}")
    @ResponseBody
    public Result<Void> remove(@PathVariable Long id) {
        categoryAdminService.remove(id);
        return Result.ok("分类已删除", (Void) null);
    }

    private void fill(CategoryForm form, Category category) {
        form.setId(category.getId());
        form.setParentId(category.getParentId() == null ? 0L : category.getParentId());
        form.setName(category.getName());
        form.setDescn(category.getDescn());
        form.setSort(category.getSort());
        form.setVisible(category.getVisible() != null && category.getVisible() == 1 ? "1" : null);
    }
}
