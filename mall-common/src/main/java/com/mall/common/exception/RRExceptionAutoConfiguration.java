package com.mall.common.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 全局异常处理器自动注册。
 *
 * <p>各服务默认包扫描覆盖不到 com.mall.common（RRExceptionHandler 所在），
 * 不注册的话 RRException 等业务异常会直接变成裸 500。
 * 通过 Spring Boot AutoConfiguration 机制统一注册（同 MemberJwtAutoConfiguration）。</p>
 */
@AutoConfiguration
@Import(RRExceptionHandler.class)
public class RRExceptionAutoConfiguration {
}
