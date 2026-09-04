package com.mall.weapp.app;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * mall-weapp 测试基类
 * 排除 DataSource 自动配置（mall-weapp 不需要直连数据库）
 * 测试环境禁用 Nacos（不依赖外部注册中心/配置中心）
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.cloud.nacos.config.enabled=false",
    "spring.config.import=optional:none:",
})
public abstract class BaseWeappTest {
}
