package com.mall.auth.perm.config;

import com.mall.auth.perm.interceptor.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 权限模块MVC配置
 * 注册权限校验拦截器，拦截 /perm/** 路径
 */
@Configuration
@RequiredArgsConstructor
public class PermWebMvcConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/sys/**")
                .excludePathPatterns("/sys/login", "/sys/refresh");
    }
}
