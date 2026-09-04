package com.mall.auth.fegin;

import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("mall-third-party")
public interface ThirdPartFeignService {
    @GetMapping(value = "/sms/sendCode")
    Result<Object> sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code);
}
