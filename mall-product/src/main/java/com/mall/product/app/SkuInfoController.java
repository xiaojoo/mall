package com.mall.product.app;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.product.entity.SkuInfoEntity;
import com.mall.product.service.CategoryService;
import com.mall.product.service.SkuInfoService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import com.mall.product.dto.SkuInfoQueryDto;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * sku信息
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@RestController
@RequestMapping("api/product/skuinfo")
@RequiredArgsConstructor
public class SkuInfoController {

    private final SkuInfoService skuInfoService;
    private final CategoryService categoryService;

    /**
     * 远程调用，根据skuId查询当前商品的价格
     */
    @GetMapping(value = "/{skuId}/price")
    public BigDecimal getPrice(@PathVariable Long skuId) {
        // 获取当前商品的信息
        SkuInfoEntity skuInfo = skuInfoService.getById(skuId);
        // 获取商品的价格
        BigDecimal price = skuInfo.getPrice();
        return price;
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(SkuInfoQueryDto query) {
        PageUtils page = skuInfoService.queryPageByCondition(query.toMap());

        return Result.success(page);
    }

    /**
     * SKU 搜索（库存添加/发布页下拉选择用），必须携带非空关键字
     */
    @GetMapping("/search")
    public Result<PageUtils> search(@RequestParam(value = "key", required = false) String key) {
        // required=false：缺失/空串统一返回业务码 400（项目约定：业务错误 HTTP 200 + code 字段）
        if (key == null || key.trim().isEmpty()) {
            return Result.fail(400, "搜索关键字不能为空");
        }
        SkuInfoQueryDto query = new SkuInfoQueryDto();
        query.setPage("1");
        query.setLimit("20");
        query.setKey(key.trim());
        PageUtils page = skuInfoService.queryPageByCondition(query.toMap());
        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{skuId}")
    public Result<SkuInfoEntity> info(@PathVariable Long skuId) {
        SkuInfoEntity skuInfo = skuInfoService.getById(skuId);

        return Result.success(skuInfo);
    }

    /**
     * SKU 详情（含图集与 SPU 商品细节图 decript，供商品管理展开查看）
     */
    @GetMapping("/detail/{skuId}")
    public Result<Map<String, Object>> detail(@PathVariable Long skuId) {
        return Result.success(skuInfoService.getSkuDetail(skuId));
    }

    /**
     * SKU 分类路径（树形回显用）：返回 skuId/skuName/分类路径数组
     */
    @GetMapping("/path/{skuId}")
    public Result<Map<String, Object>> path(@PathVariable Long skuId) {
        SkuInfoEntity sku = skuInfoService.getById(skuId);
        Map<String, Object> map = new HashMap<>();
        if (sku == null) {
            map.put("skuId", skuId);
            map.put("skuName", null);
            map.put("catalogPath", new Long[0]);
            return Result.success(map);
        }
        Long[] path = categoryService.findCatelogPath(sku.getCatalogId());
        map.put("skuId", sku.getSkuId());
        map.put("skuName", sku.getSkuName());
        map.put("catalogPath", path);
        return Result.success(map);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SkuInfoEntity skuInfo) {
        skuInfoService.save(skuInfo);

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody SkuInfoEntity skuInfo) {
        skuInfoService.updateById(skuInfo);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] skuIds) {
        skuInfoService.removeByIds(Arrays.asList(skuIds));

        return Result.success();
    }

}
