package com.mall.coupon.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mall.coupon.entity.SeckillSkuRelationEntity;
import com.mall.coupon.service.SeckillSkuRelationService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;



/**
 * 秒杀活动商品关联
 *
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
@RestController
@RequestMapping("coupon/seckillskurelation")
@RequiredArgsConstructor
public class SeckillSkuRelationController {

    private final SeckillSkuRelationService seckillSkuRelationService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params){
        PageUtils page = seckillSkuRelationService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<SeckillSkuRelationEntity> info(@PathVariable Long id){
		SeckillSkuRelationEntity seckillSkuRelation = seckillSkuRelationService.getById(id);

        return Result.success(seckillSkuRelation);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SeckillSkuRelationEntity seckillSkuRelation){
		seckillSkuRelationService.save(seckillSkuRelation);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody SeckillSkuRelationEntity seckillSkuRelation){
		seckillSkuRelationService.updateById(seckillSkuRelation);

        return Result.success();
    }

    /**
     * 下架/上架：DB 先行持久化 shelf_status + Redis 同步。
     * 下架=库存置 0（商城展示「已下架」不可抢购）；上架/补库存=按 DB 最新配置刷新并重置库存
     */
    @PostMapping("/shelf")
    public Result<Void> shelf(@RequestParam("id") Long id,
                              @RequestParam("shelf") Boolean shelf) {
        seckillSkuRelationService.updateShelfStatus(id, shelf);
        return Result.success();
    }

}
