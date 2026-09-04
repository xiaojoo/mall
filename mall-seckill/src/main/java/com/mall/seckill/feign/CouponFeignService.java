package com.mall.seckill.feign;

import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("mall-coupon")
public interface CouponFeignService {
    @GetMapping(value = "/coupon/seckillsession/Lates3DaySession")
    Result<Object> getLates3DaySession();
}
