package com.mall.seckill.controller.api;

import com.mall.common.exception.RRException;
import com.mall.common.utils.Result;
import com.mall.seckill.service.SeckillService;
import com.mall.seckill.to.SeckillSkuRedisTo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 秒杀 API 接口（前后端分离）
 * <p>
 * 替代原模板引擎页面接口：秒杀数据查询与秒杀下单（SeckillController 页面版）。
 * </p>
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillApiController {

    private final SeckillService seckillService;

    /**
     * 当前时间可以参与秒杀的商品列表
     */
    @GetMapping("/current")
    public Result<List<SeckillSkuRedisTo>> current() {
        return Result.success(seckillService.getCurrentSeckillSkus());
    }

    /**
     * 秒杀商品分场次：live=正在秒杀（在场场次可抢商品） / upcoming=预约秒杀（下一场） / history=历史秒杀（已结束场次 + 售罄/下架商品）
     */
    @GetMapping("/sessions")
    public Result<Map<String, List<SeckillSkuRedisTo>>> sessions() {
        return Result.success(seckillService.getSeckillSessions());
    }

    /**
     * 根据 skuId 查询商品是否参加秒杀活动
     *
     * @param skuId 商品 SKU ID
     */
    @GetMapping("/sku/{skuId}")
    public Result<SeckillSkuRedisTo> skuInfo(@PathVariable Long skuId) {
        return Result.success(seckillService.getSkuSeckilInfo(skuId));
    }

    /**
     * 秒杀下单，成功返回订单号 orderSn（前端凭 orderSn 调起支付）
     * <p>
     * 业务校验失败（已抢购/未开始/库存不足等）抛 {@link RRException}，
     * 由全局 {@code RRExceptionHandler} 统一转为 Result 返回，此处不重复处理。
     *
     * @param killId 秒杀活动商品 ID
     * @param key    秒杀随机码（防攻击）
     * @param num    秒杀数量
     */
    @PostMapping("/kill")
    public Result<String> kill(@RequestParam("killId") String killId,
                               @RequestParam("key") String key,
                               @RequestParam("num") Integer num) {
        return Result.success(seckillService.kill(killId, key, num));
    }
}
