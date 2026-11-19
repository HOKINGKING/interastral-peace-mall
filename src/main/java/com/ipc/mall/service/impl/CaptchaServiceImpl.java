package com.ipc.mall.service.impl;

import com.ipc.mall.common.constant.CaptchaScope;
import com.ipc.mall.service.CaptchaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;

/**
 * Java2D 手写验证码（PRD §5.7）：不新增 Hutool/Kaptcha 依赖。
 * <p>
 * 字符集去掉了容易混淆的 0/O/1/l/I；校验忽略大小写，5 分钟有效。
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final long TTL_MILLIS = 5 * 60 * 1000L;
    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private static final Color[] INK_COLORS = {
            new Color(67, 56, 202),
            new Color(29, 78, 216),
            new Color(15, 118, 110),
            new Color(180, 83, 9),
            new Color(190, 24, 93)
    };

    private final SecureRandom random = new SecureRandom();

    @Override
    public BufferedImage generate(HttpSession session, CaptchaScope scope) {
        String code = randomCode();
        session.setAttribute(scope.getCodeKey(), code);
        session.setAttribute(scope.getExpireKey(), System.currentTimeMillis() + TTL_MILLIS);
        return draw(code);
    }

    @Override
    public boolean verify(HttpSession session, CaptchaScope scope, String input) {
        Object codeValue = session.getAttribute(scope.getCodeKey());
        Object expireValue = session.getAttribute(scope.getExpireKey());

        // 一次性：先移除，再判断，确保失败也不能重复使用
        session.removeAttribute(scope.getCodeKey());
        session.removeAttribute(scope.getExpireKey());

        if (!(codeValue instanceof String code) || !(expireValue instanceof Number expire)) {
            return false;
        }
        if (input == null || input.isBlank()) {
            return false;
        }
        if (System.currentTimeMillis() > expire.longValue()) {
            return false;
        }
        return code.equalsIgnoreCase(input.trim());
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return code.toString();
    }

    private BufferedImage draw(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 248, 255));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 噪点 30 个
            for (int i = 0; i < 30; i++) {
                g.setColor(randomColor(170, 220));
                int x = random.nextInt(WIDTH);
                int y = random.nextInt(HEIGHT);
                g.fillOval(x, y, 2, 2);
            }

            // 干扰线 3 条
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 3; i++) {
                g.setColor(randomColor(120, 200));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(INK_COLORS[random.nextInt(INK_COLORS.length)]);
                double angle = (random.nextDouble() - 0.5) * 0.35;
                int x = 16 + i * 25;
                int y = 29 + random.nextInt(5) - 2;
                AffineTransform old = g.getTransform();
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.setTransform(old);
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    private Color randomColor(int min, int max) {
        int bound = max - min;
        return new Color(min + random.nextInt(bound), min + random.nextInt(bound), min + random.nextInt(bound));
    }
}
