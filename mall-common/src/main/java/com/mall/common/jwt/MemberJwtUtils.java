package com.mall.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 会员端（C 端）JWT 工具：签发/解析会员登录态
 * - 配置：member.jwt.secret / member.jwt.expire-seconds / member.jwt.header（nacos 配置中心覆盖）
 * - 与后台管理端（mall-auth perm 模块 auth.jwt）分开，独立 secret，互不通用
 * - 签发方：mall-auth（微博 OAuth/账号密码登录）、mall-third-party（微信 OAuth）
 * - 消费方：mall-order / mall-cart / mall-member 等业务服务的登录拦截器
 * - 单 token 方案（有效期默认 7 天）；如需可吊销可扩展为双 token + Redis 黑名单
 * - 通过 MemberJwtAutoConfiguration 自动注册（各服务默认扫描不到 com.mall.common）
 */
@ConfigurationProperties(prefix = "member.jwt")
public class MemberJwtUtils {

    /** 签名密钥（HS256 要求 ≥ 32 字节，生产环境务必通过 nacos 覆盖） */
    private String secret = "YOUR_MEMBER_JWT_SECRET_AT_LEAST_32_BYTES";

    /** 有效期（秒），默认 7 天 */
    private long expireSeconds = 604800;

    /** token 请求头名称（兼容现状：mall-ui 用 token header） */
    private String header = "token";

    /**
     * 签发会员 JWT
     */
    public String generateToken(Long memberId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireSeconds * 1000))
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析会员 JWT：签名/过期校验，成功返回 memberId，失败返回 null
     */
    public Long parseMemberId(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(getKey()).build()
                    .parseSignedClaims(token).getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从请求中提取 token：优先 header（默认 token），兼容 query 参数
     */
    public String extractToken(HttpServletRequest request) {
        String token = request.getHeader(header);
        if (StringUtils.isBlank(token)) {
            token = request.getParameter(header);
        }
        return token;
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}
