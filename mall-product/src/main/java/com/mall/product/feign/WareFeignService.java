package com.mall.product.feign;

import com.mall.common.vo.SkuHasStockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("mall-ware")
public interface WareFeignService {
    @PostMapping("ware/waresku/hasstock")
    List<SkuHasStockVo> getSkuHasStock(@RequestBody List<Long> skuIds);
}
