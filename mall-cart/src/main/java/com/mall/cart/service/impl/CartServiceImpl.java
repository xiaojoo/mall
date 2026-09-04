package com.mall.cart.service.impl;

import com.alibaba.fastjson2.JSON;
import com.mall.cart.exception.CartExceptionHandler;
import com.mall.cart.vo.CartVo;
import com.mall.cart.vo.SkuInfoVo;
import com.mall.cart.vo.SpuInfoVo;
import com.mall.common.utils.Result;
import com.mall.cart.feign.ProductFeignService;
import com.mall.cart.interceptor.CartInterceptor;
import com.mall.cart.service.CartService;
import com.mall.cart.to.UserInfoTo;
import com.mall.cart.vo.CartItemVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static com.mall.common.constant.CartConstant.CART_PREFIX;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final StringRedisTemplate redisTemplate;


    private final ProductFeignService productFeignService;


    private final ThreadPoolExecutor executor;

    @Override
    public void addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        addToCart(skuId, num, null);
    }

    @Override
    public void addToCart(Long skuId, Integer num, String skuAttrValues) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        // 判断Redis是否有该商品的信息
        String productRedisValue = (String) cartOps.get(skuId.toString());
        // 如果没有就添加数据
        CartItemVo cartItemVo;
        if (StringUtils.isEmpty(productRedisValue)) {
            // 2、添加新的商品到购物车(redis)
            cartItemVo = new CartItemVo();

            // 开启第一个异步任务
            CompletableFuture<Void> getSkuInfoFuture = CompletableFuture.runAsync(() -> {
                // 1、远程查询当前要添加商品的信息
                Result<SkuInfoVo> productSkuInfo = productFeignService.getInfo(skuId);
                SkuInfoVo skuInfo = productSkuInfo.getData();
                if (skuInfo == null) {
                    throw new RuntimeException("未查询到商品信息，skuId=" + skuId);
                }
                // 数据赋值操作
                cartItemVo.setSkuId(skuInfo.getSkuId());
                cartItemVo.setTitle(skuInfo.getSkuTitle());
                cartItemVo.setImage(skuInfo.getSkuDefaultImg());
                cartItemVo.setPrice(skuInfo.getPrice());
                cartItemVo.setCount(num);
                // 查询商家(品牌)名称
                try {
                    Result<SpuInfoVo> spuInfo = productFeignService.getSpuInfoBySkuId(skuId);
                    SpuInfoVo spu = spuInfo != null ? spuInfo.getData() : null;
                    if (spu != null) {
                        cartItemVo.setBrandName(spu.getBrandName());
                    }
                } catch (Exception e) {
                    log.warn("获取品牌信息失败, skuId={}", skuId, e);
                }
            }, executor);

            // 开启第二个异步任务
            CompletableFuture<Void> getSkuAttrValuesFuture = CompletableFuture.runAsync(() -> {
                // 2、远程查询skuAttrValues组合信息（详情页已传规格时优先使用传入值）
                if (StringUtils.isNotBlank(skuAttrValues)) {
                    cartItemVo.setSkuAttrValues(
                            java.util.Arrays.stream(skuAttrValues.split(";"))
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .collect(java.util.stream.Collectors.toList()));
                } else {
                    List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                    cartItemVo.setSkuAttrValues(skuSaleAttrValues);
                }
            }, executor);

            // 等待所有的异步任务全部完成
            CompletableFuture.allOf(getSkuInfoFuture, getSkuAttrValuesFuture).get();
            String cartItemJson = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), cartItemJson);
        } else {
            // 购物车有此商品，修改数量即可
            cartItemVo = JSON.parseObject(productRedisValue, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount() + num);
            // 传入规格时同步更新规格（详情页重新选择后加购）
            if (StringUtils.isNotBlank(skuAttrValues)) {
                cartItemVo.setSkuAttrValues(
                        java.util.Arrays.stream(skuAttrValues.split(";"))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(java.util.stream.Collectors.toList()));
            }
            // 修改redis的数据
            String cartItemJson = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), cartItemJson);
        }
    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        // 拿到要操作的购物车信息
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String redisValue = (String) cartOps.get(skuId.toString());
        return JSON.parseObject(redisValue, CartItemVo.class);
    }

    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {
        CartVo cartVo = new CartVo();
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        if (userInfoTo.getUserId() != null) {
            // 1、登录
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            // 临时购物车的键
            String temptCartKey = CART_PREFIX + userInfoTo.getUserKey();
            // 2、如果临时购物车的数据还未进行合并
            List<CartItemVo> tempCartItems = getCartItems(temptCartKey);
            if (tempCartItems != null) {
                // 临时购物车有数据需要进行合并操作
                for (CartItemVo item : tempCartItems) {
                    addToCart(item.getSkuId(), item.getCount());
                }
                // 清除临时购物车的数据
                clearCartInfo(temptCartKey);
            }
            // 3、获取登录后的购物车数据【包含合并过来的临时购物车的数据和登录后购物车的数据】
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cartVo.setItems(cartItems);
        } else {
            // 没登录
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            // 获取临时购物车里面的所有购物项
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cartVo.setItems(cartItems);
        }
        return cartVo;
    }

    @Override
    public void clearCartInfo(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    /**
     * 清空当前用户的购物车（按当前线程用户解析 cartKey）
     */
    @Override
    public void clearCart() {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        redisTemplate.delete(cartOps.getKey());
    }

    /**
     * 批量删除当前用户购物车中的指定商品
     */
    @Override
    public void deleteCartItems(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        Object[] keys = skuIds.stream().map(String::valueOf).toArray();
        cartOps.delete(keys);
    }

    /**
     * 商品是否选中
     */
    @Override
    public void checkItem(Long skuId, Integer checked) {
        //查询购物车里面的商品
        CartItemVo cartItem = getCartItem(skuId);
        // 修改商品状态
        cartItem.setCheck(checked == 1);
        // 序列化存入redis中
        String redisValue = JSON.toJSONString(cartItem);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), redisValue);
    }

    /**
     * 改变商品数量
     */
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        changeItemCount(skuId, num, null);
    }

    @Override
    public void changeItemCount(Long skuId, Integer num, String skuAttrValues) {
        // 查询购物车里面的商品
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        // 传入规格时同步更新规格（购物车弹窗更换选择）
        if (StringUtils.isNotBlank(skuAttrValues)) {
            cartItem.setSkuAttrValues(
                    java.util.Arrays.stream(skuAttrValues.split(";"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(java.util.stream.Collectors.toList()));
        }
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        // 序列化存入redis中
        String redisValue = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), redisValue);
    }

    /**
     * 删除商品信息
     */
    @Override
    public void deleteIdCartInfo(Integer skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    /**
     * 获取当前用户购物车全部商品（购物车页面展示，不区分是否选中）
     */
    @Override
    public List<CartItemVo> getUserCartItems() {
        // 获取当前用户登录的信息
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        // 如果用户未登录直接返回null
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            // 获取购物车项
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            // 获取所有的
            List<CartItemVo> cartItems = getCartItems(cartKey);
            if (cartItems == null) {
                throw new CartExceptionHandler();
            }
            return cartItems.stream()
                    .map(item -> {
                        // 更新为最新的价格（查询数据库）
                        BigDecimal price = productFeignService.getPrice(item.getSkuId());
                        item.setPrice(price);
                        // 老数据没有品牌名：补一次并写回 redis
                        if (StringUtils.isBlank(item.getBrandName())) {
                            try {
                                Result<SpuInfoVo> spuInfo = productFeignService.getSpuInfoBySkuId(item.getSkuId());
                                SpuInfoVo spu = spuInfo != null ? spuInfo.getData() : null;
                                if (spu != null) {
                                    item.setBrandName(spu.getBrandName());
                                    BoundHashOperations<String, Object, Object> ops = getCartOps();
                                    ops.put(item.getSkuId().toString(), JSON.toJSONString(item));
                                }
                            } catch (Exception e) {
                                log.warn("补充品牌信息失败, skuId={}", item.getSkuId(), e);
                            }
                        }
                        return item;
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取当前用户购物车中选中的商品项（订单结算用，原 /currentUserCartItems）
     */
    @Override
    public List<CartItemVo> getCheckedCartItems() {
        // 获取当前用户登录的信息
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        // 如果用户未登录直接返回null
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            // 获取购物车项
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            // 获取所有的
            List<CartItemVo> cartItems = getCartItems(cartKey);
            if (cartItems == null) {
                throw new CartExceptionHandler();
            }
            // 筛选出所有选中的购物项
            return cartItems.stream()
                    .filter(items -> items.getCheck())
                    .map(item -> {
                        // 更新为最新的价格（查询数据库）
                        BigDecimal price = productFeignService.getPrice(item.getSkuId());
                        item.setPrice(price);
                        return item;
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取购物车里面的数据
     */
    private List<CartItemVo> getCartItems(String cartKey) {
        // 获取购物车里面的所有商品
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        List<Object> values = operations.values();
        if (values != null && !values.isEmpty()) {
            return values.stream().map((obj) -> {
                String str = (String) obj;
                return JSON.parseObject(str, CartItemVo.class);
            }).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 获取到我们要操作的购物车
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        // 先得到当前用户信息
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            // mall:cart:1
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        // 绑定指定的key操作Redis
        return redisTemplate.boundHashOps(cartKey);
    }
}
