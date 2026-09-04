package com.mall.auth.app.controller.api;

import com.mall.auth.app.entity.UserEntity;
import com.mall.auth.app.form.LoginForm;
import com.mall.auth.app.service.UserService;
import com.mall.auth.fegin.MemberFeignService;
import com.mall.auth.vo.UserLoginVo;
import com.mall.auth.vo.UserRegisterVo;
import com.mall.common.constant.AuthConstant;
import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 认证 API 接口（前后端分离）
 * <p>
 * 登录/注册对接会员体系（mall-member），前端以会员 ID 作为登录凭证（token），
 * 供订单等服务的 API 层按 token 解析登录用户。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final UserService userService;

    private final MemberFeignService memberFeignService;

    private final StringRedisTemplate redisTemplate;

    private final MemberJwtUtils memberJwtUtils;

    /**
     * 获取图形验证码：返回 key + base64 图片，5 分钟内有效
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        String key = UUID.randomUUID().toString().replace("-", "");
        String code = randomCaptchaCode();
        redisTemplate.opsForValue().set(AuthConstant.CAPTCHA_CODE_PREFIX + key, code, 5, TimeUnit.MINUTES);
        Map<String, String> data = new HashMap<>();
        data.put("key", key);
        data.put("img", drawCaptchaImage(code));
        return Result.success(data);
    }

    /**
     * 会员登录（商城前端），返回会员信息 + 会员 JWT（前端以 data.token 作为登录凭证）
     */
    @PostMapping("/login")
    public Result<Object> login(@RequestBody LoginForm form) {
        // 校验图形验证码（一次性，用后即删）
        Result<Object> captchaCheck = validateCaptcha(form.getCaptchaKey(), form.getCaptchaCode());
        if (captchaCheck != null) {
            return captchaCheck;
        }
        UserLoginVo vo = new UserLoginVo();
        vo.setLoginUser(form.getMobile());
        vo.setPassword(form.getPassword());

        Result<Object> login = memberFeignService.login(vo);
        if (login.getCode() == 200) {
            Map<String, Object> data = new HashMap<>();
            if (login.getData() instanceof Map<?, ?> memberMap) {
                memberMap.forEach((k, v) -> data.put(String.valueOf(k), v));
            }
            // 签发会员 JWT 作为登录凭证（不再用明文 memberId）
            Object idObj = data.get("id");
            if (idObj != null) {
                data.put("token", memberJwtUtils.generateToken(Long.valueOf(idObj.toString())));
            }
            return Result.success(data);
        }
        return Result.fail(login.getCode(), login.getMessage());
    }

    /**
     * 会员注册（商城前端）：先校验图形验证码 + 短信验证码（Redis 比对，令牌机制），通过后注册会员
     */
    @PostMapping("/register")
    public Result<Object> register(@RequestBody UserRegisterVo vo) {
        // 1、校验图形验证码
        Result<Object> captchaCheck = validateCaptcha(vo.getCaptchaKey(), vo.getCaptchaCode());
        if (captchaCheck != null) {
            return captchaCheck;
        }
        // 2、校验短信验证码
        String code = vo.getCode();
        String redisCode = redisTemplate.opsForValue().get(AuthConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (StringUtils.isEmpty(redisCode) || StringUtils.isEmpty(code) || !code.equals(redisCode.split("_")[0])) {
            return Result.fail("验证码错误");
        }
        // 3、删除短信验证码，防止重复使用
        redisTemplate.delete(AuthConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());

        // 4、调用会员服务注册
        Result<Object> register = memberFeignService.register(vo);
        if (register.getCode() == 200) {
            return Result.success();
        }
        return Result.fail(register.getCode(), register.getMessage());
    }

    /**
     * 校验图形验证码，通过返回 null，失败返回错误 Result（用后即删）
     */
    private Result<Object> validateCaptcha(String captchaKey, String captchaCode) {
        if (StringUtils.isEmpty(captchaKey) || StringUtils.isEmpty(captchaCode)) {
            return Result.fail("请输入图形验证码");
        }
        String redisCode = redisTemplate.opsForValue().get(AuthConstant.CAPTCHA_CODE_PREFIX + captchaKey);
        log.debug("图形验证码校验：key={}, 输入={}, Redis值={}", captchaKey, captchaCode, redisCode);
        if (StringUtils.isEmpty(redisCode) || !redisCode.equalsIgnoreCase(captchaCode)) {
            return Result.fail("图形验证码错误");
        }
        redisTemplate.delete(AuthConstant.CAPTCHA_CODE_PREFIX + captchaKey);
        return null;
    }

    // 生成 4 位验证码（去除易混淆字符）
    private String randomCaptchaCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // 绘制验证码图片，返回 base64（PNG）
    private String drawCaptchaImage(String code) {
        int width = 110;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Random random = new Random();
        // 背景
        g.setColor(new Color(242, 244, 247));
        g.fillRect(0, 0, width, height);
        // 干扰线
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.drawLine(random.nextInt(width), random.nextInt(height), random.nextInt(width), random.nextInt(height));
        }
        // 字符
        int step = width / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(random.nextInt(120) + 40, random.nextInt(120) + 40, random.nextInt(120) + 40));
            g.setFont(new Font("Arial", Font.BOLD, 22 + random.nextInt(4)));
            g.drawString(String.valueOf(code.charAt(i)), step * (i + 1) - 8, height / 2 + 8);
        }
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            log.error("生成验证码图片失败", e);
            return "";
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public Result<UserEntity> info(@RequestParam Long userId) {
        UserEntity user = userService.getById(userId);
        return Result.success(user);
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody UserEntity user) {
        userService.updateById(user);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PostMapping("/updatePassword")
    public Result<Void> updatePassword(@RequestParam Long userId, @RequestParam String password) {
        userService.updatePassword(userId, password);
        return Result.success();
    }

    /**
     * 退出登录（登录态由前端清除，userId 可选；token 存续期自然过期）
     */
    @GetMapping("/logout")
    public Result<Void> logout(@RequestParam(required = false) Long userId) {
        // 退出登录逻辑
        return Result.success();
    }
}
