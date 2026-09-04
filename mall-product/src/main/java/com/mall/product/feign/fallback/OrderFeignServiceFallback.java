package com.mall.product.feign.fallback;

import com.mall.common.utils.Result;
import com.mall.product.feign.OrderFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单服务不可用时拒绝评价（fail-closed）：
 * 购买校验是硬规则，宁可误伤也不放开限制。
 */
@Slf4j
@Component
public class OrderFeignServiceFallback implements OrderFeignService {

    @Override
    public Result<Boolean> paidCheck(Long memberId, Long skuId, Long spuId) {
        log.error("OrderFeignService.paidCheck 降级拒绝: memberId={}, skuId={}, spuId={}", memberId, skuId, spuId);
        return Result.success(Boolean.FALSE);
    }
}
