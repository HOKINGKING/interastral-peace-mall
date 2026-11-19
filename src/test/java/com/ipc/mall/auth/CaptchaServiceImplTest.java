package com.ipc.mall.auth;

import com.ipc.mall.common.constant.CaptchaScope;
import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.service.impl.CaptchaServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证码规则（PRD §5.7）：4 位、忽略大小写、一次性、5 分钟过期、前后台 key 隔离。
 */
class CaptchaServiceImplTest {

    private final CaptchaServiceImpl captchaService = new CaptchaServiceImpl();

    @Test
    void 生成图片并写入四位字符() {
        MockHttpSession session = new MockHttpSession();
        BufferedImage image = captchaService.generate(session, CaptchaScope.FRONT);

        assertNotNull(image);
        assertEquals(120, image.getWidth());
        assertEquals(40, image.getHeight());

        String code = (String) session.getAttribute(SessionKey.CAPTCHA_CODE);
        assertNotNull(code);
        assertEquals(4, code.length());
        assertNotNull(session.getAttribute(SessionKey.CAPTCHA_EXPIRE_AT));
    }

    @Test
    void 校验忽略大小写且一次后失效() {
        MockHttpSession session = new MockHttpSession();
        captchaService.generate(session, CaptchaScope.FRONT);
        String code = (String) session.getAttribute(SessionKey.CAPTCHA_CODE);

        assertTrue(captchaService.verify(session, CaptchaScope.FRONT, code.toLowerCase()));
        assertNull(session.getAttribute(SessionKey.CAPTCHA_CODE));
        assertNull(session.getAttribute(SessionKey.CAPTCHA_EXPIRE_AT));
        assertFalse(captchaService.verify(session, CaptchaScope.FRONT, code));
    }

    @Test
    void 过期验证码失败且被清理() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKey.CAPTCHA_CODE, "ABCD");
        session.setAttribute(SessionKey.CAPTCHA_EXPIRE_AT, System.currentTimeMillis() - 1);

        assertFalse(captchaService.verify(session, CaptchaScope.FRONT, "ABCD"));
        assertNull(session.getAttribute(SessionKey.CAPTCHA_CODE));
        assertNull(session.getAttribute(SessionKey.CAPTCHA_EXPIRE_AT));
    }

    @Test
    void 前后台验证码互不影响() {
        MockHttpSession session = new MockHttpSession();
        captchaService.generate(session, CaptchaScope.FRONT);
        captchaService.generate(session, CaptchaScope.ADMIN);

        String adminCode = (String) session.getAttribute(SessionKey.ADMIN_CAPTCHA_CODE);

        // 用后台验证码去校验前台：前台验证码无论成败都会失效，后台验证码不受影响
        assertFalse(captchaService.verify(session, CaptchaScope.FRONT, adminCode));
        assertNull(session.getAttribute(SessionKey.CAPTCHA_CODE));
        assertEquals(adminCode, session.getAttribute(SessionKey.ADMIN_CAPTCHA_CODE));
        assertTrue(captchaService.verify(session, CaptchaScope.ADMIN, adminCode));
    }
}
