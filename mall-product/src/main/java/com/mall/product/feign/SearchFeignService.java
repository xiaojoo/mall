package com.mall.product.feign;

import com.mall.common.to.es.SkuEsModel;
import com.mall.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("mall-search")
public interface SearchFeignService {
    @PostMapping("/search/save/product")
    Result<Object> productStatusUp(@RequestBody List<SkuEsModel> skuEsModels);

    /**
     * 下架：删除 ES 中对应 sku 的商品文档
     */
    @PostMapping("/search/save/product/delete")
    Result<Object> productDown(@RequestBody List<Long> skuIds);
}
