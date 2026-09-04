package com.mall.thirdparty.config;

import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.List;

/**
 * 网络工具配置类
 * 解决 Spring Cloud 3.x 中 InetUtils / InetUtilsProperties Bean 缺失问题
 */
@Configuration
public class NetworkConfig {

    @Bean
    @Primary
    public InetUtilsProperties inetUtilsProperties() {
        InetUtilsProperties properties = new InetUtilsProperties();
        // 忽略虚拟网络接口，避免获取到错误的 IP
        List<String> ignoredInterfaces = Arrays.asList("docker0", "veth.*", "br-*", "lo");
        properties.setIgnoredInterfaces(ignoredInterfaces);
        // 设置获取本地 IP 的超时时间（秒）
        properties.setTimeoutSeconds(5);
        return properties;
    }

    @Bean
    @Primary
    public InetUtils inetUtils(InetUtilsProperties properties) {
        return new InetUtils(properties);
    }
}
