package com.mall.weapp.feign;

import com.mall.common.utils.Result;
import com.mall.weapp.feign.fallback.CartFeignServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 购物车服务远程调用接口
 * <p>通过OpenFeign调用mall-cart服务的购物车相关接口</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@FeignClient(value = "mall-cart", fallback = CartFeignServiceFallback.class)
public interface CartFeignService {

    /**
     * 获取当前用户的购物车列表
     *
     * @return 购物车列表
     */
    @GetMapping("cart/cart/list")
    Result<Object> list();

    /**
     * 添加商品到购物车
     *
     * @param skuId SKU ID
     * @param num   数量
     * @return 操作结果
     */
    @PostMapping("cart/cart/add")
    Result<Object> addToCart(@RequestParam("skuId") Long skuId,
                             @RequestParam("num") Integer num);

    /**
     * 更新购物车中商品数量
     *
     * @param skuId SKU ID
     * @param num   新数量
     * @return 操作结果
     */
    @PostMapping("cart/cart/update")
    Result<Object> updateCart(@RequestParam("skuId") Long skuId,
                              @RequestParam("num") Integer num);

    /**
     * 删除购物车中指定的商品
     *
     * @param skuIds 要删除的SKU ID数组
     * @return 操作结果
     */
    @PostMapping("cart/cart/delete")
    Result<Object> deleteCart(@RequestParam("skuIds") Long[] skuIds);
}
