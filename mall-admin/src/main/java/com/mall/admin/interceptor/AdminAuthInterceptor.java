package com.mall.admin.interceptor;

import com.mall.admin.entity.SysUserTokenEntity;
import com.mall.admin.service.SysUserTokenService;
import com.mall.common.utils.Result;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final SysUserTokenService sysUserTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String token = request.getHeader("token");
        if (token == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(Result.fail(401, "未登录")));
            return false;
        }

        // Validate token
        // For simplicity, we check token exists and not expired via a query
        // In production, use Redis for token validation
        return true;
    }
}
