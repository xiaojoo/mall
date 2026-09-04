package com.mall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mall.common.valid.AddGroup;
import com.mall.common.valid.UpdateGroup;
import com.mall.common.valid.UpdateStatusGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.mall.product.entity.BrandEntity;
import com.mall.product.service.BrandService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.mall.product.dto.BrandQueryDto;

/**
 * 品牌
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@Slf4j
@RestController
@RequestMapping("api/product/brand")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(BrandQueryDto query) {
        PageUtils page = brandService.queryPage(query.toMap());

        return Result.success(page);
    }

    /**
     * search模块远程调用
     * 品牌所有信息
     */
    @GetMapping("/infos")
    public Result<Object> infos(@RequestParam("brandIds") List<Long> brandIds) {
        List<BrandEntity> brand = brandService.getBrandsByIds(brandIds);

        return Result.success(brand);
    }

    /**
     * 信息
     */
    @GetMapping("/info/{brandId}")
    public Result<BrandEntity> info(@PathVariable Long brandId) {
        BrandEntity brand = brandService.getById(brandId);

        return Result.success(brand);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand) {
        brandService.save(brand);

        return Result.success();
    }

    /**
     * 批量添加（单条失败不影响其他条，返回每条结果）
     */
    @PostMapping("/batchSave")
    public Result<Object> batchSave(@RequestBody List<BrandEntity> brands) {
        try {
            return Result.success(brandService.batchSave(brands));
        } catch (Exception e) {
            log.error("批量添加品牌失败", e);
            return Result.fail(500, "批量添加失败：" + e.getMessage());
        }
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand) {
        brandService.updateDetail(brand);

        return Result.success();
    }

    /**
     * 修改状态
     */
    @PostMapping("/update/status")
    public Result<Object> updateStatus(@Validated({UpdateStatusGroup.class}) @RequestBody BrandEntity brand) {
        brandService.updateById(brand);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] brandIds) {
        brandService.removeByIds(Arrays.asList(brandIds));

        return Result.success();
    }

}
