package com.mall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.product.entity.SkuSaleAttrValueEntity;
import com.mall.product.service.SkuSaleAttrValueService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.common.dto.PageQueryDTO;



/**
 * sku销售属性&值
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@RestController
@RequestMapping("api/product/skusaleattrvalue")
@RequiredArgsConstructor
public class SkuSaleAttrValueController {

    private final SkuSaleAttrValueService skuSaleAttrValueService;

    /**
     * 购物车远程调用
     */
    @GetMapping(value = "/stringList/{skuId}")
    public List<String> getSkuSaleAttrValues(@PathVariable Long skuId) {
        return skuSaleAttrValueService.getSkuSaleAttrValuesAsStringList(skuId);
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(@RequestParam Map<String, Object> params){
        PageUtils page = skuSaleAttrValueService.queryPage(params);

        return Result.success(page);
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<SkuSaleAttrValueEntity> info(@PathVariable Long id){
		SkuSaleAttrValueEntity skuSaleAttrValue = skuSaleAttrValueService.getById(id);

        return Result.success(skuSaleAttrValue);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SkuSaleAttrValueEntity skuSaleAttrValue){
		skuSaleAttrValueService.save(skuSaleAttrValue);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody SkuSaleAttrValueEntity skuSaleAttrValue){
		skuSaleAttrValueService.updateById(skuSaleAttrValue);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids){
		skuSaleAttrValueService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
