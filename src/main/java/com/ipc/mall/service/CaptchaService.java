package com.ipc.mall.service;

import com.ipc.mall.common.constant.CaptchaScope;
import jakarta.servlet.http.HttpSession;

import java.awt.image.BufferedImage;

/**
 * 图形验证码服务（PRD §5.7）。
 */
public interface CaptchaService {

    /**
     * 生成验证码图片，并把字符与过期时间写入 Session。
     */
    BufferedImage generate(HttpSession session, CaptchaScope scope);

    /**
     * 校验验证码。无论成功失败，校验后立即失效（一次性）。
     */
    boolean verify(HttpSession session, CaptchaScope scope, String input);
}
