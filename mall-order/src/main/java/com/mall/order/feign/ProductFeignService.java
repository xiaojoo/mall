package com.mall.order.feign;

import com.mall.common.utils.Result;
import com.mall.order.vo.SkuInfoVo;
import com.mall.order.vo.SpuInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient("mall-product")
public interface ProductFeignService {

    /**
     * 根据skuId查询spu的信息
     */
    @GetMapping(value = "/api/product/spuinfo/skuId/{skuId}")
    Result<SpuInfoVo> getSpuInfoBySkuId(@PathVariable Long skuId);

    /**
     * 批量按 skuId 查询 spu 信息（品牌补全用，替代逐个调用）
     *
     * @return skuId -> spu 信息
     */
    @PostMapping(value = "/api/product/spuinfo/batchSpuInfoBySkuIds")
    Result<Map<Long, SpuInfoVo>> getSpuInfoMapBySkuIds(@RequestBody List<Long> skuIds);

    /**
     * 根据skuId查询sku信息（立即购买直购模式补全商品信息用）
     */
    @GetMapping(value = "/api/product/skuinfo/info/{skuId}")
    Result<SkuInfoVo> getSkuInfo(@PathVariable Long skuId);

    /**
     * 根据skuId查询销售属性（如 颜色：黑 / 内存：256G），秒杀建单补全商品参数用
     */
    @GetMapping(value = "/api/product/skusaleattrvalue/stringList/{skuId}")
    List<String> getSkuSaleAttrValues(@PathVariable Long skuId);

}