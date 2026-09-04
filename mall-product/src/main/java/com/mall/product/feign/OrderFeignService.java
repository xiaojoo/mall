package com.mall.product.feign;

import com.mall.common.utils.Result;
import com.mall.product.feign.fallback.OrderFeignServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 订单服务 Feign：商品评论购买校验
 */
@FeignClient(value = "mall-order", fallback = OrderFeignServiceFallback.class)
public interface OrderFeignService {

    /**
     * 校验会员是否购买过某商品且已支付成功
     */
    @GetMapping("/api/order/paid/check")
    Result<Boolean> paidCheck(@RequestParam("memberId") Long memberId,
                              @RequestParam(value = "skuId", required = false) Long skuId,
                              @RequestParam(value = "spuId", required = false) Long spuId);
}
