package com.mall.weapp.feign.fallback;

import com.mall.common.utils.Result;
import com.mall.weapp.feign.OrderFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单服务Feign熔断降级
 * <p>当mall-order服务不可用时，返回错误信息避免级联故障</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@Slf4j
@Component
public class OrderFeignServiceFallback implements OrderFeignService {

    @Override
    public Result<Object> list(Map<String, Object> params) {
        log.warn("订单服务调用失败，返回空列表");
        return Result.fail("订单服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Object> info(Long id) {
        log.warn("订单服务调用失败，获取订单详情为空");
        return Result.fail("订单服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Object> createOrder(String cartIds, Long addressId) {
        log.warn("订单服务调用失败，创建订单失败");
        return Result.fail("订单服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Object> cancel(Long id) {
        log.warn("订单服务调用失败，取消订单失败");
        return Result.fail("订单服务暂时不可用，请稍后重试");
    }
}
