package com.mall.weapp.feign;

import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import com.mall.weapp.feign.fallback.ProductFeignServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 商品服务远程调用接口
 * <p>通过OpenFeign调用mall-product服务的商品相关接口</p>
 *
 * @author mall
 * @date 2024-08-01
 */
@FeignClient(value = "mall-product", fallback = ProductFeignServiceFallback.class)
public interface ProductFeignService {

    /**
     * 分页查询商品列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    @GetMapping("api/product/spuinfo/list")
    Result<Object> list(@RequestParam Map<String, Object> params);

    /**
     * 根据ID查询商品详情
     *
     * @param id 商品SPU ID
     * @return 商品详情
     */
    @GetMapping("api/product/spuinfo/info/{id}")
    Result<Object> info(@PathVariable Long id);

    /**
     * 查询商品分类树
     *
     * @return 分类列表
     */
    @GetMapping("api/product/category/list/tree")
    Result<Object> categoryList();

    /**
     * 根据SKU ID查询SPU信息
     *
     * @param skuId SKU ID
     * @return SPU信息
     */
    @GetMapping("api/product/spuinfo/skuId/{skuId}")
    Result<Object> getSpuInfoBySkuId(@PathVariable Long skuId);

    /**
     * 查询SKU信息
     *
     * @param skuId SKU ID
     * @return SKU详情
     */
    @GetMapping("api/product/skuinfo/info/{skuId}")
    Result<Object> skuInfo(@PathVariable Long skuId);
}
