package com.mall.common.jackson;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

import java.util.Date;

/**
 * Jackson 全局自动配置。
 *
 * <p>各服务默认包扫描覆盖不到 com.mall.common，通过 Spring Boot AutoConfiguration
 * 机制统一注册（同 MemberJwtAutoConfiguration）。</p>
 *
 * <p>注册宽松的 java.util.Date 反序列化器：兼容前端提交的 ISO-8601 UTC 字符串
 * （2026-08-18T16:00:00.000Z）与现有 yyyy-MM-dd HH:mm:ss 格式，避免
 * HttpMessageNotReadableException（Unparseable date）报错。</p>
 */
@AutoConfiguration
public class JacksonAutoConfiguration {

    @Bean
    public JacksonModule lenientDateModule() {
        SimpleModule module = new SimpleModule("LenientDateModule");
        module.addDeserializer(Date.class, new LenientDateDeserializer());
        return module;
    }
}
