package com.ipc.mall.controller;

import com.ipc.mall.common.constant.CaptchaScope;
import com.ipc.mall.service.CaptchaService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * 图形验证码接口（PRD §5.7）：前台 /captcha，后台 /admin/captcha。
 */
@Controller
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping(value = "/captcha", produces = MediaType.IMAGE_PNG_VALUE)
    public void frontCaptcha(HttpSession session, HttpServletResponse response) throws IOException {
        writeCaptcha(session, response, CaptchaScope.FRONT);
    }

    @GetMapping(value = "/admin/captcha", produces = MediaType.IMAGE_PNG_VALUE)
    public void adminCaptcha(HttpSession session, HttpServletResponse response) throws IOException {
        writeCaptcha(session, response, CaptchaScope.ADMIN);
    }

    private void writeCaptcha(HttpSession session, HttpServletResponse response, CaptchaScope scope)
            throws IOException {
        // 禁止缓存，避免浏览器复用旧图；前端仍会加时间戳刷新
        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        BufferedImage image = captchaService.generate(session, scope);
        ImageIO.write(image, "png", response.getOutputStream());
    }
}
