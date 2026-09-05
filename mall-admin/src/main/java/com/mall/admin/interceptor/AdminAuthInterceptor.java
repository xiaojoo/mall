package com.mall.admin.interceptor;

import com.mall.common.utils.Result;
import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * mall-admin 接口鉴权：校验 mall-auth 签发的 JWT（auth.jwt.secret）。
 * 之前只判断 token!=null 形同未鉴权，现改为签名 + 过期 + 类型校验，非法一律 401。
 */
@Slf4j
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Value("${auth.jwt.secret:YOUR_AUTH_JWT_SECRET_AT_LEAST_32_BYTES}")
    private String jwtSecret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String token = request.getHeader("token");
        if (token == null || token.isBlank()) {
            return reject(response);
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            Claims claims = jws.getPayload();
            if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
                return reject(response);
            }
            // refresh 类型 token 不允许调用业务接口
            Object type = claims.get("type");
            if ("refresh".equals(type)) {
                return reject(response);
            }
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                return reject(response);
            }
        } catch (Exception e) {
            log.warn("JWT 校验失败: " + e.getMessage());
            return reject(response);
        }
        return true;
    }

    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(Result.fail(401, "未登录")));
        return false;
    }
}
