package com.mall.member.feign;

import com.mall.common.utils.Result;
import com.mall.member.vo.SpuFavVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * 商品服务远程调用（会员收藏聚合）
 */
@FeignClient("mall-product")
public interface ProductFeignService {

    /**
     * 批量查询 SPU 收藏展示信息：名称/主图/首个 SKU 价格/分类名
     *
     * @param spuIds SPU id 列表
     * @return spuId -> 展示信息
     */
    @PostMapping("/api/product/spuinfo/favInfo")
    Result<Map<Long, SpuFavVo>> favInfo(@RequestBody List<Long> spuIds);
}
