package com.mall.weapp.feign.fallback;

import com.mall.common.utils.Result;
import com.mall.weapp.feign.CartFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 购物车服务Feign熔断降级
 * <p>当mall-cart服务不可用时，返回错误信息避免级联故障</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@Slf4j
@Component
public class CartFeignServiceFallback implements CartFeignService {

    @Override
    public Result<Object> list() {
        log.warn("购物车服务调用失败，返回空列表");
        return Result.fail("购物车服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Object> addToCart(Long skuId, Integer num) {
        log.warn("购物车服务调用失败，添加购物车失败");
        return Result.fail("购物车服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Object> updateCart(Long skuId, Integer num) {
        log.warn("购物车服务调用失败，更新购物车失败");
        return Result.fail("购物车服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Object> deleteCart(Long[] skuIds) {
        log.warn("购物车服务调用失败，删除购物车项失败");
        return Result.fail("购物车服务暂时不可用，请稍后重试");
    }
}
