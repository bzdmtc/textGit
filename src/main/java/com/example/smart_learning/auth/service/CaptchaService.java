package com.example.smart_learning.auth.service;

import com.example.smart_learning.auth.dto.CaptchaResponse;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码：4 位字符，图片以 Base64 data URL 返回，答案存 Redis（2 分钟，一次性）。
 */
@Service
public class CaptchaService {

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final Duration TTL = Duration.ofMinutes(2);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final StringRedisTemplate redis;

    public CaptchaService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public CaptchaResponse create() {
        StringBuilder code = new StringBuilder(4);
        for (int i = 0; i < 4; i += 1) {
            code.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        String id = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(key(id), code.toString(), TTL);
        return new CaptchaResponse(id, "data:image/png;base64," + render(code.toString()));
    }

    public void verify(String id, String input) {
        if (id == null || id.isBlank() || input == null || input.isBlank()) {
            throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED);
        }
        String expected = redis.opsForValue().get(key(id));
        if (expected == null) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        if (!expected.equalsIgnoreCase(input.trim())) {
            // 输错即作废，前端需要刷新图片
            redis.delete(key(id));
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        redis.delete(key(id));
    }

    private String key(String id) {
        return "auth:captcha:" + id;
    }

    private String render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(248, 250, 252));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            for (int i = 0; i < 6; i += 1) {
                g.setColor(randomColor(150, 215));
                g.setStroke(new BasicStroke(1.2f));
                g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                        RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            }
            // 噪点
            for (int i = 0; i < 40; i += 1) {
                g.setColor(randomColor(160, 225));
                g.fillRect(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 2, 2);
            }
            // 字符
            g.setFont(new Font("Arial", Font.BOLD, 26));
            for (int i = 0; i < code.length(); i += 1) {
                g.setColor(randomColor(30, 120));
                AffineTransform original = g.getTransform();
                double angle = (RANDOM.nextDouble() - 0.5) * 0.45;
                g.rotate(angle, 18 + i * 26, HEIGHT / 2.0);
                g.drawString(String.valueOf(code.charAt(i)), 12 + i * 26, 30);
                g.setTransform(original);
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render captcha", ex);
        }
    }

    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(min + RANDOM.nextInt(range), min + RANDOM.nextInt(range), min + RANDOM.nextInt(range));
    }
}
