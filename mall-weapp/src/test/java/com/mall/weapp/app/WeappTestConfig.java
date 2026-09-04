package com.mall.weapp.app;

import com.mall.weapp.feign.CartFeignService;
import com.mall.weapp.feign.MemberFeignService;
import com.mall.weapp.feign.OrderFeignService;
import com.mall.weapp.feign.ProductFeignService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
class WeappTestConfig {

    @Bean
    ProductFeignService productFeignService() {
        return mock(ProductFeignService.class);
    }

    @Bean
    CartFeignService cartFeignService() {
        return mock(CartFeignService.class);
    }

    @Bean
    MemberFeignService memberFeignService() {
        return mock(MemberFeignService.class);
    }

    @Bean
    OrderFeignService orderFeignService() {
        return mock(OrderFeignService.class);
    }
}
