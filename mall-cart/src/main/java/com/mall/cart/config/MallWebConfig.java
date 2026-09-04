package com.mall.cart.config;

import com.mall.cart.interceptor.CartInterceptor;
import com.mall.common.jwt.MemberJwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class MallWebConfig implements WebMvcConfigurer {

    private final MemberJwtUtils memberJwtUtils;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CartInterceptor(memberJwtUtils)) // 注册拦截器
                .addPathPatterns("/**");
    }
}
