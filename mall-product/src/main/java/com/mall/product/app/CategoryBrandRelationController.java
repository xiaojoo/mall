package com.mall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.product.entity.BrandEntity;
import com.mall.product.vo.BrandVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.product.entity.CategoryBrandRelationEntity;
import com.mall.product.service.CategoryBrandRelationService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;


/**
 * 品牌分类关联
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@RestController
@RequestMapping("api/product/categorybrandrelation")
@RequiredArgsConstructor
public class CategoryBrandRelationController {

    private final CategoryBrandRelationService categoryBrandRelationService;

    /**
     * 根据catId查出品牌
     */
    @GetMapping("/brands/list")
    public Result<Object> relationBrandList(@RequestParam(value = "catId", required = true) Long catId) {
        List<BrandEntity> vos = categoryBrandRelationService.getBrandsByCatId(catId);
        List<BrandVo> collect = vos.stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    BrandVo brandVo = new BrandVo();
                    brandVo.setBrandId(item.getBrandId());
                    brandVo.setBrandName(item.getName());
                    return brandVo;
                })
                .collect(Collectors.toList());
        return Result.success(collect);
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params) {
        PageUtils page = categoryBrandRelationService.queryPage(params);

        return Result.success(page);
    }

    /**
     * 获取当前品牌关联的所有分类列表
     */
    @GetMapping("/catelog/list")
    public Result<Object> cateloglist(@RequestParam("brandId") Long brandId) {
        List<CategoryBrandRelationEntity> data = categoryBrandRelationService
                .list(new LambdaQueryWrapper<CategoryBrandRelationEntity>().eq(CategoryBrandRelationEntity::getBrandId, brandId));

        return Result.success(data);
    }

    /**
     * 保存
     */
    @RequestMapping("/catelog/save")
    public Result<Object> Catelogsave(@RequestBody CategoryBrandRelationEntity categoryBrandRelation) {
        categoryBrandRelationService.saveDetail(categoryBrandRelation);

        return Result.success();
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<CategoryBrandRelationEntity> info(@PathVariable Long id) {
        CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return Result.success(categoryBrandRelation);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation) {
        categoryBrandRelationService.save(categoryBrandRelation);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation) {
        categoryBrandRelationService.updateById(categoryBrandRelation);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
