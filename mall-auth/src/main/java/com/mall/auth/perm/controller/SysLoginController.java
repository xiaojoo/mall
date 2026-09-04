package com.mall.auth.perm.controller;

import com.mall.auth.perm.entity.SysUserEntity;
import com.mall.auth.perm.jwt.JwtTokenService;
import com.mall.auth.perm.jwt.JwtTokenService.TokenPair;
import com.mall.auth.perm.service.SysUserService;
import com.mall.common.exception.RRException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 系统登录接口（前端 /api/sys/login → /sys/login）
 * 认证方案：JWT 双 token + 黑名单（access 短效 30min + refresh 长效 7d 可吊销）
 */
@RestController
@RequestMapping("/sys")
@RequiredArgsConstructor
@Tag(name = "系统登录", description = "登录/登出/刷新/用户信息")
public class SysLoginController {

    private final SysUserService sysUserService;
    private final StringRedisTemplate redisTemplate;
    private final JwtTokenService jwtTokenService;

    /**
     * 登录
     * 前端期望返回 { code: 0, token: "xxx", refreshToken: "xxx", expiresIn: 1800 }
     */
    @PostMapping("/login")
    @Operation(summary = "系统用户登录")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        // 校验图形验证码（与 /captcha.jpg 存储前缀 captcha: 一致，用后即删）
        String captchaKey = params.get("uuid");
        String captchaCode = params.get("captcha");
        if (StringUtils.isEmpty(captchaKey) || StringUtils.isEmpty(captchaCode)) {
            throw new RRException("请输入图形验证码");
        }
        String redisCode = redisTemplate.opsForValue().get("captcha:" + captchaKey);
        if (StringUtils.isEmpty(redisCode) || !redisCode.equalsIgnoreCase(captchaCode)) {
            throw new RRException("图形验证码错误");
        }
        redisTemplate.delete("captcha:" + captchaKey);

        String username = params.get("username");
        String password = params.get("password");

        if (username == null || password == null) {
            throw new RRException("用户名或密码不能为空");
        }

        SysUserEntity user = sysUserService.queryByUsername(username);
        if (user == null) {
            throw new RRException("用户不存在");
        }
        if (user.getStatus() != 1) {
            throw new RRException("账号已被禁用");
        }
        if (!new BCryptPasswordEncoder().matches(password, user.getPassword())) {
            throw new RRException("密码错误");
        }

        // JWT 双 token
        TokenPair pair = jwtTokenService.issueTokens(user.getUserId(), user.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("token", pair.token());
        result.put("refreshToken", pair.refreshToken());
        result.put("expiresIn", pair.expiresIn());
        return result;
    }

    /**
     * 刷新双 token（access 过期后由前端自动调用）
     * 请求体 { refreshToken }；成功返回新的 token/refreshToken/expiresIn
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌（双 token 旋转）")
    public Map<String, Object> refresh(@RequestBody Map<String, String> params) {
        String refreshToken = params.get("refreshToken");
        TokenPair pair = jwtTokenService.refresh(refreshToken);
        Map<String, Object> result = new HashMap<>();
        if (pair == null) {
            result.put("code", 401);
            result.put("msg", "登录已失效，请重新登录");
            return result;
        }
        result.put("code", 0);
        result.put("token", pair.token());
        result.put("refreshToken", pair.refreshToken());
        result.put("expiresIn", pair.expiresIn());
        return result;
    }

    /**
     * 获取当前登录用户信息
     * userId 由 PermissionInterceptor 从 JWT 解析后放入 request attribute
     * 前端期望返回 { code: 0, user: { username, ... } }
     */
    @GetMapping("/user/info")
    @Operation(summary = "获取当前用户信息")
    public Map<String, Object> userInfo(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        Map<String, Object> result = new HashMap<>();

        if (userIdObj == null) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        Long userId = Long.parseLong(userIdObj.toString());
        SysUserEntity user = sysUserService.getById(userId);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "用户不存在");
            return result;
        }

        // 清除敏感信息
        user.setPassword(null);
        user.setSalt(null);

        result.put("code", 0);
        result.put("user", user);
        return result;
    }

    /**
     * 退出登录（走拦截器校验后进入）：access token 拉黑 + 吊销 refresh
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Map<String, Object> logout(HttpServletRequest request) {
        jwtTokenService.logout(jwtTokenService.extractToken(request));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }
}
