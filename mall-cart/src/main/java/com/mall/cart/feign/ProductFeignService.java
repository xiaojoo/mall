package com.mall.cart.feign;

import com.mall.cart.vo.SkuInfoVo;
import com.mall.cart.vo.SpuInfoVo;
import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@FeignClient("mall-product")
public interface ProductFeignService {

    /**
     * 根据skuId查询sku信息
     */
    @RequestMapping("/api/product/skuinfo/info/{skuId}")
    Result<SkuInfoVo> getInfo(@PathVariable Long skuId);

    /**
     * 根据skuId查询spu信息（获取品牌名）
     */
    @GetMapping("/api/product/spuinfo/skuId/{skuId}")
    Result<SpuInfoVo> getSpuInfoBySkuId(@PathVariable Long skuId);
    /**
     * 根据skuId查询pms_sku_sale_attr_value表中的信息
     */
    @GetMapping(value = "/api/product/skusaleattrvalue/stringList/{skuId}")
    List<String> getSkuSaleAttrValues(@PathVariable Long skuId);

    /**
     * 根据skuId查询当前商品的最新价格
     */
    @GetMapping(value = "/api/product/skuinfo/{skuId}/price")
    BigDecimal getPrice(@PathVariable Long skuId);
}
