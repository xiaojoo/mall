package com.mall.auth.perm.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.mall.auth.perm.dao.SysUserDao;
import com.mall.auth.perm.entity.SysUserEntity;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 双 token + 黑名单混合方案：
 * - access token：短有效期（默认 30 分钟），每次请求校验签名 + 黑名单
 * - refresh token：长有效期（默认 7 天），存 Redis 可吊销，刷新时旋转（旧 refresh 立即失效）
 * - 黑名单：登出时将 access token 的 jti 写入 Redis，TTL = 剩余有效期
 *
 * Redis Key 设计：
 *   auth:refresh:{userId}  → 当前有效的 refresh jti（登出/旋转时删除/覆盖，实现"主动失效"）
 *   auth:blacklist:{jti}   → 已吊销的 access token（TTL = 剩余有效期）
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    public static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    public static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";
    public static final String DISABLED_KEY_PREFIX = "auth:disabled:";
    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    public static final String CLAIM_USERNAME = "username";

    private final JwtProperties props;
    private final StringRedisTemplate redisTemplate;
    private final SysUserDao sysUserDao;

    /**
     * 签发双 token（覆盖式旋转：同一用户旧 refresh 立即失效）
     */
    public TokenPair issueTokens(Long userId, String username) {
        // 清除历史踢人/禁用标记（用户已重新登录成功）
        redisTemplate.delete(DISABLED_KEY_PREFIX + userId);

        long now = System.currentTimeMillis();
        Date nowDate = new Date(now);
        Date accessExp = new Date(now + props.getAccessExpireSeconds() * 1000);
        Date refreshExp = new Date(now + props.getRefreshExpireSeconds() * 1000);
        SecretKey key = getKey();

        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        String accessToken = Jwts.builder()
                .id(accessJti)
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_USERNAME, username)
                .issuedAt(nowDate)
                .expiration(accessExp)
                .signWith(key)
                .compact();

        String refreshToken = Jwts.builder()
                .id(refreshJti)
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_USERNAME, username)
                .issuedAt(nowDate)
                .expiration(refreshExp)
                .signWith(key)
                .compact();

        redisTemplate.opsForValue().set(REFRESH_KEY_PREFIX + userId, refreshJti,
                Duration.ofSeconds(props.getRefreshExpireSeconds()));

        return new TokenPair(accessToken, refreshToken, props.getAccessExpireSeconds());
    }

    /**
     * 校验 access token：签名/过期/类型/黑名单/禁用标记，全部通过返回 userId，否则 null
     */
    public Long parseAccessUserId(String token) {
        AuthUser user = parseAccessUser(token);
        return user == null ? null : user.userId();
    }

    /**
     * 校验 access token 并返回 userId + username（用于日志等场景）
     */
    public AuthUser parseAccessUser(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            return null;
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + claims.getId()))) {
            return null;
        }
        // 踢人/禁用标记：命中即失效（TTL = refresh 有效期）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(DISABLED_KEY_PREFIX + claims.getSubject()))) {
            return null;
        }
        return new AuthUser(Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_USERNAME, String.class));
    }

    /**
     * 刷新：校验 refresh token 与 Redis 中记录一致后，旋转签发新双 token
     * 失败返回 null（refresh 过期/被吊销/不匹配）
     */
    public TokenPair refresh(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return null;
        }
        Claims claims = parse(refreshToken);
        if (claims == null) {
            return null;
        }
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            return null;
        }
        Long userId = Long.valueOf(claims.getSubject());
        // 被踢/禁用后不允许再刷新
        if (Boolean.TRUE.equals(redisTemplate.hasKey(DISABLED_KEY_PREFIX + userId))) {
            return null;
        }
        String storedJti = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (storedJti == null || !storedJti.equals(claims.getId())) {
            return null;
        }
        // refresh token 未携带 username claim（历史令牌）时回查数据库兜底，
        // 否则续签出的 access token 无用户名 → 操作日志 username 恒为空
        String username = claims.get(CLAIM_USERNAME, String.class);
        if (username == null) {
            SysUserEntity user = sysUserDao.selectById(userId);
            if (user != null) {
                username = user.getUsername();
            }
        }
        return issueTokens(userId, username);
    }

    /**
     * 登出：access token 进黑名单（TTL=剩余有效期），删除该用户 refresh 记录
     */
    public void logout(String accessToken) {
        if (StringUtils.isBlank(accessToken)) {
            return;
        }
        Claims claims = parse(accessToken);
        if (claims == null) {
            return;
        }
        long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remaining > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + claims.getId(), "1",
                    Duration.ofMillis(remaining));
        }
        redisTemplate.delete(REFRESH_KEY_PREFIX + claims.getSubject());
    }

    /**
     * 踢人/禁用即时生效：写入禁用标记（TTL=refresh 有效期）并吊销 refresh，
     * 该用户所有已签发的 access token 立即失效且无法再刷新，只能重新登录
     */
    public void kickUser(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.opsForValue().set(DISABLED_KEY_PREFIX + userId, "1",
                Duration.ofSeconds(props.getRefreshExpireSeconds()));
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
    }

    /**
     * 从请求中提取 token：优先 header（默认 token），兼容 Authorization: Bearer
     */
    public String extractToken(HttpServletRequest request) {
        String token = request.getHeader(props.getHeader());
        if (StringUtils.isBlank(token)) {
            String auth = request.getHeader("Authorization");
            if (StringUtils.isNotBlank(auth) && auth.startsWith("Bearer ")) {
                token = auth.substring(7);
            }
        }
        return token;
    }

    private Claims parse(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        try {
            return Jwts.parser().verifyWith(getKey()).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 双 token 返回体
     */
    public record TokenPair(String token, String refreshToken, long expiresIn) {
    }

    /**
     * 已认证用户（拦截器写入 request attribute 用）
     */
    public record AuthUser(Long userId, String username) {
    }
}
