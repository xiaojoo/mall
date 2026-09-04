package com.mall.auth.perm.interceptor;

import com.alibaba.fastjson2.JSON;
import com.mall.auth.perm.annotation.RequirePermission;
import com.mall.auth.perm.jwt.JwtTokenService;
import com.mall.auth.perm.service.SysUserService;
import com.mall.common.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 权限校验拦截器
 * 1. JWT 认证：解析 access token（签名/过期/类型/黑名单），userId 写入 request attribute
 * 2. 根据 @RequirePermission 注解校验当前用户是否具有相应权限
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    public static final String USER_KEY = "userId";

    private final SysUserService sysUserService;
    private final JwtTokenService jwtTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 1. JWT 认证（所有 /sys/** 请求，除登录/刷新外）
        String token = jwtTokenService.extractToken(request);
        JwtTokenService.AuthUser authUser = StringUtils.isBlank(token) ? null : jwtTokenService.parseAccessUser(token);
        if (authUser == null) {
            writeError(response, 401, "未登录或登录已失效，请重新登录");
            return false;
        }
        request.setAttribute(USER_KEY, authUser.userId());
        request.setAttribute("username", authUser.username());

        // 2. 接口级权限校验
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            return true;
        }

        String[] requiredPerms = annotation.value();
        RequirePermission.Logical logical = annotation.logical();
        Long userId = authUser.userId();

        // 超级管理员拥有全部权限
        if (userId == 1L) {
            return true;
        }

        // 查询用户权限列表
        List<String> userPerms = sysUserService.queryPermsList(userId);

        // 超级管理员通配符
        if (userPerms.contains("*:*:*")) {
            return true;
        }

        boolean hasPermission;
        if (logical == RequirePermission.Logical.AND) {
            hasPermission = Arrays.stream(requiredPerms).allMatch(userPerms::contains);
        } else {
            hasPermission = Arrays.stream(requiredPerms).anyMatch(userPerms::contains);
        }

        if (!hasPermission) {
            log.warn("权限校验失败: userId={}, required={}", userId, Arrays.toString(requiredPerms));
            writeError(response, 403, "权限不足，无法执行此操作");
            return false;
        }

        return true;
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(Result.fail(code, message)));
    }
}
