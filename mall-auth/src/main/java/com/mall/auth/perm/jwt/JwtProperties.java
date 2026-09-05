package com.mall.auth.perm.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（双 token + 黑名单混合方案）
 * 配置项：auth.jwt.*（application.yml / nacos mall-auth.yaml 均可覆盖）
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    /** 签名密钥（HS256 要求 ≥ 32 字节）。
     *  唯一默认源在此；mall-auth 与 mall-admin 共用：通过环境变量 AUTH_JWT_SECRET 统一覆盖（Spring 松弛绑定 auth.jwt.secret），
     *  切勿在两个 nacos 配置文件里各写一份不同值，否则导致签名/校验不一致。 */
    private String secret = "YOUR_AUTH_JWT_SECRET_AT_LEAST_32_BYTES";

    /** access token 有效期（秒），默认 30 分钟 */
    private long accessExpireSeconds = 1800;

    /** refresh token 有效期（秒），默认 7 天 */
    private long refreshExpireSeconds = 604800;

    /** 前端携带 token 的 header 名（兼容现状：mall-web 用 token header） */
    private String header = "token";
}
