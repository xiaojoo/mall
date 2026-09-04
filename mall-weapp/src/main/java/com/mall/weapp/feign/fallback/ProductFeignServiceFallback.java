package com.mall.weapp.feign.fallback;

import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.weapp.feign.ProductFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 商品服务Feign熔断降级
 * <p>当mall-product服务不可用时，返回空数据避免级联故障</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@Slf4j
@Component
public class ProductFeignServiceFallback implements ProductFeignService {

    @Override
    public Result<Object> list(Map<String, Object> params) {
        log.warn("商品服务调用失败，返回空列表");
        return Result.fail("商品服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Object> info(Long id) {
        log.warn("商品服务调用失败，获取商品详情为空");
        return Result.fail("商品服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Object> categoryList() {
        log.warn("商品服务调用失败，返回空分类");
        return Result.fail("商品服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Object> getSpuInfoBySkuId(Long skuId) {
        log.warn("商品服务调用失败，获取SPU信息为空");
        return Result.fail("商品服务暂时不可用，请稍后重试");
    }

    @Override
    public Result<Object> skuInfo(Long skuId) {
        log.warn("商品服务调用失败，获取SKU信息为空");
        return Result.fail("商品服务暂时不可用，请稍后重试");
    }
}
