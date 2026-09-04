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

import com.mall.coupon.entity.CouponSpuCategoryRelationEntity;
import com.mall.coupon.service.CouponSpuCategoryRelationService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;



/**
 * 优惠券分类关联
 *
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
@RestController
@RequestMapping("coupon/couponspucategoryrelation")
@RequiredArgsConstructor
public class CouponSpuCategoryRelationController {

    private final CouponSpuCategoryRelationService couponSpuCategoryRelationService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params){
        PageUtils page = couponSpuCategoryRelationService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<CouponSpuCategoryRelationEntity> info(@PathVariable Long id){
		CouponSpuCategoryRelationEntity couponSpuCategoryRelation = couponSpuCategoryRelationService.getById(id);

        return Result.success(couponSpuCategoryRelation);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody CouponSpuCategoryRelationEntity couponSpuCategoryRelation){
		couponSpuCategoryRelationService.save(couponSpuCategoryRelation);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody CouponSpuCategoryRelationEntity couponSpuCategoryRelation){
		couponSpuCategoryRelationService.updateById(couponSpuCategoryRelation);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids){
		couponSpuCategoryRelationService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
