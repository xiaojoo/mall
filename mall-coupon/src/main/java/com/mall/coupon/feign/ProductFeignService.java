package com.mall.coupon.feign;

import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 商品服务远程调用（可用优惠券按 SKU 过滤：查 sku 分类与分类树）
 */
@FeignClient("mall-product")
public interface ProductFeignService {

    /**
     * sku 基本信息（含 catalogId 叶子分类）
     */
    @RequestMapping("/api/product/skuinfo/info/{skuId}")
    Result<Object> getSkuInfo(@PathVariable Long skuId);

    /**
     * 全部分类树（三级）
     */
    @GetMapping("/api/product/category/list/tree")
    Result<Object> getCategoryTree();
}
