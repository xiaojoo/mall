package com.mall.seckill.controller;

import com.mall.common.utils.Result;
import com.mall.seckill.service.SeckillService;
import com.mall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 当前时间可以参与秒杀的商品信息
     */
    @GetMapping(value = "/getCurrentSeckillSkus")
    @ResponseBody
    public Result<Object> getCurrentSeckillSkus() {

        // 获取到当前可以参加秒杀商品的信息
        List<SeckillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();

        return Result.success().setData(vos);
    }

    /**
     * 根据skuId查询商品是否参加秒杀活动
     */
    @GetMapping(value = "/sku/seckill/{skuId}")
    @ResponseBody
    public Result<Object> getSkuSeckilInfo(@PathVariable Long skuId) {

        SeckillSkuRedisTo to = seckillService.getSkuSeckilInfo(skuId);

        return Result.success().setData(to);
    }

    /**
     * 商品进行秒杀(秒杀开始)
     * 原 /kill 返回 success 模板页面，已由 API 接口取代，此处移除模板渲染
     */
}
