package com.mall.cart.service;

import com.mall.cart.vo.CartItemVo;
import com.mall.cart.vo.CartVo;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    /**
     * 将商品添加至购物车
     */
    void addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 添加商品到购物车（可指定规格，缺省由后端按 SKU 默认）
     */
    void addToCart(Long skuId, Integer num, String skuAttrValues) throws ExecutionException, InterruptedException;

    /**
     * 获取购物车某个购物项
     */
    CartItemVo getCartItem(Long skuId);

    /**
     * 获取购物车里面的信息
     */
    CartVo getCart() throws ExecutionException, InterruptedException;

    /**
     * 清空购物车的数据
     */
    void clearCartInfo(String cartKey);

    /**
     * 清空当前用户的购物车（按当前线程用户解析 cartKey）
     */
    void clearCart();

    /**
     * 批量删除当前用户购物车中的指定商品
     */
    void deleteCartItems(List<Long> skuIds);

    /**
     * 商品是否选中
     */
    void checkItem(Long skuId, Integer checked);

    /**
     * 改变商品数量
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 更新购物车商品数量（可同步更新规格，缺省不变）
     */
    void changeItemCount(Long skuId, Integer num, String skuAttrValues);

    /**
     * 删除商品信息
     */
    void deleteIdCartInfo(Integer skuId);

    List<CartItemVo> getUserCartItems();

    /**
     * 获取当前用户购物车中选中的商品项（订单结算用）
     */
    List<CartItemVo> getCheckedCartItems();
}
