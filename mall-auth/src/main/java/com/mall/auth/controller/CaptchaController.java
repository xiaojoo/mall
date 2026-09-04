package com.mall.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
public class CaptchaController {

    private final StringRedisTemplate redisTemplate;

    public CaptchaController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/captcha.jpg")
    public void getCaptcha(@RequestParam(value = "uuid", required = false) String uuid,
                           HttpServletResponse response) throws IOException {
        if (uuid == null || uuid.isEmpty()) {
            uuid = UUID.randomUUID().toString();
        }

        // 生成验证码
        String code = generateCode();
        log.info("验证码: {} -> {}", uuid, code);

        // 存入 Redis，5分钟有效
        redisTemplate.opsForValue().set("captcha:" + uuid, code, 5, TimeUnit.MINUTES);

        // 生成图片
        int width = 280;
        int height = 90;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 背景
        g.setColor(new Color(255, 255, 255));
        g.fillRect(0, 0, width, height);

        // 干扰线
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.drawLine(random.nextInt(width), random.nextInt(height),
                       random.nextInt(width), random.nextInt(height));
        }

        // 绘制验证码文字
        g.setFont(new Font("Arial", Font.BOLD, 36));
        char[] chars = code.toCharArray();
        int x = 20;
        for (char c : chars) {
            g.setColor(new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
            // 轻微旋转
            double radian = Math.toRadians(random.nextInt(20) - 10);
            g.rotate(radian, x, 55);
            g.drawString(String.valueOf(c), x, 55);
            g.rotate(-radian, x, 55);
            x += 42;
        }

        // 噪点
        for (int i = 0; i < 50; i++) {
            g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
            g.fillOval(random.nextInt(width), random.nextInt(height), 2, 2);
        }

        g.dispose();

        // 输出图片
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        try (OutputStream os = response.getOutputStream()) {
            ImageIO.write(image, "jpg", os);
        }
    }

    private String generateCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
