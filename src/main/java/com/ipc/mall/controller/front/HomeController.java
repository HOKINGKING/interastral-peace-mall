package com.ipc.mall.controller.front;

import com.ipc.mall.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前台首页（PRD §5.1）：分类入口 + 本季推荐 + 服务承诺。
 */
@Controller
public class HomeController {

    private static final int RECOMMEND_LIMIT = 8;

    private final ProductService productService;

    public HomeController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping({"/", "/index"})
    public String home(Model model) {
        model.addAttribute("recommended", productService.recommended(RECOMMEND_LIMIT));
        return "front/index";
    }
}
