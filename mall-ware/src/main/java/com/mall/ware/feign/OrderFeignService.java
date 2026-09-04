package com.mall.ware.feign;

import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("mall-order")
public interface OrderFeignService {
    @GetMapping(value = "/order/order/status/{orderSn}")
    Result<Object> getOrderStatus(@PathVariable String orderSn);
}
