package com.mall.product.feign.fallback;

import com.mall.common.exception.BizCodeEnum;
import com.mall.common.utils.Result;
import com.mall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillFeignServiceFallback implements SeckillFeignService {
    @Override
    public Result<Object> getSkuSeckilInfo(Long skuId) {
        log.info("熔断方法调用");
        return Result.fail(BizCodeEnum.TO_MANY_REQUEST.getCode(), BizCodeEnum.TO_MANY_REQUEST.getMsg());
    }
}
