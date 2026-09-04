package com.mall.weapp.feign;

import com.mall.common.utils.Result;
import com.mall.weapp.feign.fallback.OrderFeignServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单服务远程调用接口
 * <p>通过OpenFeign调用mall-order服务的订单相关接口</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@FeignClient(value = "mall-order", fallback = OrderFeignServiceFallback.class)
public interface OrderFeignService {

    /**
     * 查询当前用户的订单列表
     *
     * @param params 查询参数
     * @return 订单列表
     */
    @GetMapping("order/order/list")
    Result<Object> list(@RequestParam Map<String, Object> params);

    /**
     * 查询订单详情
     *
     * @param id 订单ID
     * @return 订单详情
     */
    @GetMapping("order/order/info/{id}")
    Result<Object> info(@PathVariable Long id);

    /**
     * 创建订单
     *
     * @param cartIds  购物车ID列表
     * @param addressId 收货地址ID
     * @return 订单ID
     */
    @PostMapping("order/order/create")
    Result<Object> createOrder(@RequestParam("cartIds") String cartIds,
                               @RequestParam("addressId") Long addressId);

    /**
     * 取消订单
     *
     * @param id 订单ID
     * @return 操作结果
     */
    @PostMapping("order/order/cancel/{id}")
    Result<Object> cancel(@PathVariable Long id);
}
