package com.mall.ware.feign;

import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("mall-product")
public interface ProductFeignService {
    @RequestMapping("/api/product/skuinfo/info/{skuId}")
    Result<Object> info(@PathVariable Long skuId);
}
