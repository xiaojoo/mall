package com.mall.cart.controller.api;

import com.mall.cart.service.CartService;
import com.mall.cart.vo.CartItemVo;
import com.mall.cart.vo.CartVo;
import com.mall.common.utils.Result;
import com.mall.common.validator.ValidatorUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.mall.cart.dto.CartRequestDto;
import lombok.RequiredArgsConstructor;

/**
 * 购物车 API 接口 (前后端分离)
 *
 * @author sunxiaojie
 * @date 2024-08-01
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartApiController {
    private final CartService cartService;

    /**
     * 获取购物车列表（全部商品，含未选中）
     */
    @GetMapping("/list")
    public Result<List<CartItemVo>> list() {
        List<CartItemVo> list = cartService.getUserCartItems();
        return Result.success(list);
    }

    /**
     * 获取当前用户购物车中选中的商品项（订单结算远程调用）
     */
    @GetMapping("/currentUserCartItems")
    public Result<List<CartItemVo>> currentUserCartItems() {
        List<CartItemVo> list = cartService.getCheckedCartItems();
        return Result.success(list);
    }

    /**
     * 获取购物车详情
     */
    @GetMapping("/detail")
    public Result<CartVo> info() {
        CartVo cart = null;
        try {
            cart = cartService.getCart();
        } catch (Exception e) {
            return Result.fail("获取购物车详情失败：" + e.getMessage());
        }
        return Result.success(cart);
    }

    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public Result<Void> add(CartRequestDto request) {
        try {
            Long skuId = Long.valueOf(request.getSkuId());
            Integer num = Integer.valueOf(request.getNum());
            cartService.addToCart(skuId, num, request.getSkuAttrValues());
            return Result.success();
        } catch (Exception e) {
            return Result.fail("添加购物车失败：" + e.getMessage());
        }
    }

    /**
     * 更新购物车商品数量（可同步更新规格）
     */
    @PostMapping("/update")
    public Result<Void> update(CartRequestDto request) {
        Long skuId = Long.valueOf(request.getSkuId());
        Integer num = Integer.valueOf(request.getNum());
        cartService.changeItemCount(skuId, num, request.getSkuAttrValues());
        return Result.success();
    }

    /**
     * 选中/取消选中购物车商品
     */
    @PostMapping("/check")
    public Result<Void> check(CartRequestDto request) {
        Long skuId = Long.valueOf(request.getSkuId());
        Integer checked = Integer.valueOf(request.getChecked());
        cartService.checkItem(skuId, checked);
        return Result.success();
    }

    /**
     * 删除购物车商品
     */
    @PostMapping("/delete/{skuId}")
    public Result<Void> delete(@PathVariable Integer skuId) {
        cartService.deleteIdCartInfo(skuId);
        return Result.success();
    }

    /**
     * 清空购物车
     */
    @PostMapping("/clear")
    public Result<Void> clear() {
        cartService.clearCart();
        return Result.success();
    }

    /**
     * 批量删除购物车商品（订单提交后清理已下单商品）
     */
    @PostMapping("/deleteByIds")
    public Result<Void> deleteByIds(@RequestBody List<Long> skuIds) {
        cartService.deleteCartItems(skuIds);
        return Result.success();
    }
}
