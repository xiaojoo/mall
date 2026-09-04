package com.mall.order.feign;

import com.mall.common.utils.Result;
import com.mall.order.vo.WareSkuLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("mall-ware")
public interface WmsFeignService {

    /**
     * 查询sku是否有库存
     */
    @PostMapping(value = "/ware/waresku/stock")
    Result<Object> getSkuStock(@RequestBody List<Long> skuIds);
    
    /**
     * 查询运费和收货地址信息
     */
    @GetMapping(value = "/ware/wareinfo/fare")
    Result<Object> getFare(@RequestParam("addrId") Long addrId);
    
    @PostMapping(value = "/ware/waresku/lock/order")
    Result<Object> orderLockStock(@RequestBody WareSkuLockVo vo);
}
