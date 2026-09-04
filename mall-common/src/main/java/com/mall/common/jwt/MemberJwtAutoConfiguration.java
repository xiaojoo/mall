package com.mall.common.jwt;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 会员 JWT 自动配置：各服务（mall-auth/order/cart/member/third-party）默认包扫描
 * 覆盖不到 com.mall.common，通过 Spring Boot AutoConfiguration 机制统一注册。
 */
@AutoConfiguration
@EnableConfigurationProperties(MemberJwtUtils.class)
public class MemberJwtAutoConfiguration {
}
