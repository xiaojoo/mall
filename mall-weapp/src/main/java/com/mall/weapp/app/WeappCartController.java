package com.mall.weapp.app;

import com.mall.common.utils.Result;
import com.mall.weapp.feign.CartFeignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 微信小程序 - 购物车模块控制器
 * <p>提供购物车的增删改查接口，供小程序端调用</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@RestController
@RequestMapping("weapp/cart")
@RequiredArgsConstructor
public class WeappCartController {

    private final CartFeignService cartFeignService;

    /**
     * 获取当前用户的购物车列表
     *
     * @return 购物车列表
     */
    @GetMapping("/list")
    public Result<Object> list() {
        return cartFeignService.list();
    }

    /**
     * 添加商品到购物车
     *
     * @param skuId 商品SKU ID
     * @param num   添加数量
     * @return 操作结果
     */
    @PostMapping("/add")
    public Result<Object> add(@RequestParam("skuId") Long skuId,
                              @RequestParam("num") Integer num) {
        return cartFeignService.addToCart(skuId, num);
    }

    /**
     * 更新购物车中商品数量
     *
     * @param skuId 商品SKU ID
     * @param num   新数量
     * @return 操作结果
     */
    @PostMapping("/update")
    public Result<Object> update(@RequestParam("skuId") Long skuId,
                                 @RequestParam("num") Integer num) {
        return cartFeignService.updateCart(skuId, num);
    }

    /**
     * 删除购物车中指定的商品
     *
     * @param skuIds 要删除的SKU ID数组
     * @return 操作结果
     */
    @PostMapping("/delete")
    public Result<Object> delete(@RequestParam("skuIds") Long[] skuIds) {
        return cartFeignService.deleteCart(skuIds);
    }
}
