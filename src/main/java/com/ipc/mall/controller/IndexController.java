package com.ipc.mall.controller;

import com.ipc.mall.common.result.Result;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 环境自检用 Controller，验证 Spring MVC + Thymeleaf 是否正常。
 * 骨架验证通过后可以删除，或者保留 /ping 用于检查服务存活。
 */
@Controller
public class IndexController {

    /** 首页：认证模块的落地页，商品首页在 M3 替换 */
    @GetMapping({"/", "/index"})
    public String index() {
        return "front/index";
    }

    /** 自检接口：返回 JSON，验证统一返回结构可用 */
    @GetMapping("/ping")
    @ResponseBody
    public Result<String> ping() {
        return Result.ok("pong");
    }
}
