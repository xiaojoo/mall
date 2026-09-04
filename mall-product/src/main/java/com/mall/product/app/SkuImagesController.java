package com.mall.product.app;

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

import com.mall.product.entity.SkuImagesEntity;
import com.mall.product.service.SkuImagesService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;



/**
 * sku图片
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@RestController
@RequestMapping("api/product/skuimages")
@RequiredArgsConstructor
public class SkuImagesController {

    private final SkuImagesService skuImagesService;

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params){
        PageUtils page = skuImagesService.queryPage(params);

        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<SkuImagesEntity> info(@PathVariable Long id){
		SkuImagesEntity skuImages = skuImagesService.getById(id);

        return Result.success(skuImages);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SkuImagesEntity skuImages){
		skuImagesService.save(skuImages);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody SkuImagesEntity skuImages){
		skuImagesService.updateById(skuImages);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids){
		skuImagesService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
