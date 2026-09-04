package com.mall.product.feign;

import com.mall.common.to.SkuReductionTo;
import com.mall.common.to.SpuBoundTo;
import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient("mall-coupon")
public interface CouponFeignService {
    @PostMapping("coupon/spubounds/save")
    Result<Object> saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);

    @PostMapping("coupon/skufullreduction/savainfo")
    Result<Object> saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);

    /**
     * 首页轮播内容（mall-ui 首页 HERO 轮播）
     */
    @GetMapping("coupon/carousel/list")
    Result<List<Map<String, Object>>> listCarousel();

    /**
     * 首页跑马灯公告文本（mall-ui 首页 Ticker）
     */
    @GetMapping("coupon/ticker/list")
    Result<List<String>> listTicker();

    /**
     * 首页大促横条（mall-ui AppPromo）
     */
    @GetMapping("coupon/promo/list")
    Result<List<Map<String, Object>>> listPromo();

    /**
     * 页脚链接（mall-ui AppFooter 全站页脚）
     */
    @GetMapping("coupon/footerlink/list")
    Result<List<Map<String, Object>>> listFooterLink();
}
