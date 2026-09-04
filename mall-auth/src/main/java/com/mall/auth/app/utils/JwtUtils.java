package com.mall.auth.app.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * jwt工具类（app端/小程序端）
 * 配置：renren.jwt.secret / renren.jwt.expire / renren.jwt.header
 */
@ConfigurationProperties(prefix = "renren.jwt")
@Component
public class JwtUtils {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private String secret;
    private long expire;
    private String header;

    /**
     * 生成jwt token
     */
    public String generateToken(long userId) {
        Date nowDate = new Date();
        //过期时间
        Date expireDate = new Date(nowDate.getTime() + expire * 1000);

        return Jwts.builder()
                .header().add("typ", "JWT").and()
                .subject(userId + "")
                .issuedAt(nowDate)
                .expiration(expireDate)
                .signWith(getKey())
                .compact();
    }

    public Claims getClaimByToken(String token) {
        try {
            return Jwts.parser().verifyWith(getKey()).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            logger.debug("validate is token error ", e);
            return null;
        }
    }

    /**
     * token是否过期
     * @return  true：过期
     */
    public boolean isTokenExpired(Date expiration) {
        return expiration.before(new Date());
    }

    private SecretKey getKey() {
        // 未配置时给默认值，避免运行时 NPE（生产环境务必配置）
        String key = (secret == null || secret.isEmpty()) ? "YOUR_APP_JWT_SECRET_AT_LEAST_32_BYTES" : secret;
        return Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}
