package com.mall.order.feign;

import com.mall.common.utils.Result;
import com.mall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("mall-cart")
public interface CartFeignService {

    /**
     * 查询当前用户购物车选中的商品项
     */
    @GetMapping(value = "/api/cart/currentUserCartItems")
    Result<List<OrderItemVo>> getCurrentCartItems();

    /**
     * 订单提交成功后删除购物车中已下单的商品
     */
    @PostMapping(value = "/api/cart/deleteByIds")
    Result<Void> deleteCartItems(@RequestBody List<Long> skuIds);
}
