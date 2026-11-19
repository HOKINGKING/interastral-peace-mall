package com.ipc.mall.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 后台工作台占位页（M6 再接入真实统计）。
 */
@Controller
public class AdminHomeController {

    @GetMapping({"/admin", "/admin/"})
    public String index() {
        return "admin/index";
    }
}
