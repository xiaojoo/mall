package com.mall.weapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 微信小程序API网关服务启动类
 * <p>作为微信小程序的统一API入口，聚合商品、购物车、订单、会员等服务</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@EnableFeignClients(basePackages = "com.mall.weapp.feign")
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableDiscoveryClient
public class MallWeappApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallWeappApplication.class, args);
    }

}
