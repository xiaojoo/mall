package com.mall.ware.feign;

import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("mall-member")
public interface MemberFeignService {

    /**
     * 根据id获取用户地址信息
     */
    @RequestMapping("/api/member/memberreceiveaddress/info/{id}")
    Result<Object> info(@PathVariable Long id);
}
