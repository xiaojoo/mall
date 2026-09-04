package com.mall.search.feign;

import com.mall.common.utils.Result;
import com.mall.search.vo.AttrResponseVo;
import com.mall.search.vo.BrandVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("mall-product")
public interface ProductFeignService {

    @RequestMapping("/api/product/attr/info/{attrId}")
    Result<AttrResponseVo> info(@PathVariable Long attrId);

    @GetMapping("/api/product/brand/infos")
    Result<List<BrandVO>> infos(@RequestParam("brandIds") List<Long> brandIds);
}
