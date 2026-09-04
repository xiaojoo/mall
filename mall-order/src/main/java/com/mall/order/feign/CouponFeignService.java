package com.mall.order.feign;

import com.mall.common.to.CouponUseCheckTo;
import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

/**
 * 优惠券服务 Feign：订单提交时校验优惠券可用性
 */
@FeignClient("mall-coupon")
public interface CouponFeignService {

    /**
     * 优惠券使用校验：通过返回优惠金额，否则抛业务异常
     */
    @PostMapping("/coupon/coupon/internal/use/check")
    Result<BigDecimal> useCheck(@RequestBody CouponUseCheckTo to);

    /**
     * 下单成功核销优惠券
     */
    @PostMapping("/coupon/coupon/internal/use/consume")
    Result<Void> consume(@RequestBody CouponUseCheckTo to);

    /**
     * 订单取消/关闭回退优惠券
     */
    @PostMapping("/coupon/coupon/internal/use/refund")
    Result<Void> refund(@RequestBody CouponUseCheckTo to);
}
