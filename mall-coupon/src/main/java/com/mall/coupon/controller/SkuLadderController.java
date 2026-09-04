package com.mall.coupon.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mall.coupon.entity.SkuLadderEntity;
import com.mall.coupon.service.SkuLadderService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;



/**
 * 商品阶梯价格
 *
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
@RestController
@RequestMapping("coupon/skuladder")
@RequiredArgsConstructor
public class SkuLadderController {

    private final SkuLadderService skuLadderService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params){
        PageUtils page = skuLadderService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<SkuLadderEntity> info(@PathVariable Long id){
		SkuLadderEntity skuLadder = skuLadderService.getById(id);

        return Result.success(skuLadder);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SkuLadderEntity skuLadder){
		skuLadderService.save(skuLadder);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody SkuLadderEntity skuLadder){
		skuLadderService.updateById(skuLadder);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids){
		skuLadderService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
