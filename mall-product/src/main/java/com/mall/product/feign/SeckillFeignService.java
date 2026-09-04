package com.mall.product.feign;

import com.mall.common.utils.Result;
import com.mall.product.feign.fallback.SeckillFeignServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value ="mall-seckill", fallback = SeckillFeignServiceFallback.class)
public interface SeckillFeignService {
    @GetMapping(value = "/sku/seckill/{skuId}")
    Result<Object> getSkuSeckilInfo(@PathVariable Long skuId);
}
