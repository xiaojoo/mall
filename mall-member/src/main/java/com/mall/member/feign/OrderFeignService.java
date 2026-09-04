package com.mall.member.feign;

import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient("mall-order")
public interface OrderFeignService {
    /**
     * 分页查询当前登录用户的所有订单信息
     */
    @PostMapping("/order/order/listWithItem")
    public Result<Object> listWithItem(@RequestBody Map<String, Object> params);
}
