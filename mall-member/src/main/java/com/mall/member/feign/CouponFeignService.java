package com.mall.member.feign;

import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("mall-coupon")
public interface CouponFeignService {
    @RequestMapping("coupon/coupon/member/list")
    public Result test();
}
