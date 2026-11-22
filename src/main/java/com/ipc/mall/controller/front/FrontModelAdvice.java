package com.ipc.mall.controller.front;

import com.ipc.mall.service.CategoryService;
import com.ipc.mall.vo.CategoryGroupVO;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * 前台公共模型：每个前台页面都要渲染顶部导航，统一在这里提供分类树，
 * 避免每个 Controller 各写一遍、漏写导致导航为空。
 */
@ControllerAdvice(basePackages = "com.ipc.mall.controller.front")
public class FrontModelAdvice {

    private final CategoryService categoryService;

    public FrontModelAdvice(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @ModelAttribute("navGroups")
    public List<CategoryGroupVO> navGroups() {
        return categoryService.visibleGroups();
    }
}
