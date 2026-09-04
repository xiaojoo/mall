package com.mall.product.app;

import java.util.Arrays;
import java.util.Map;

import com.mall.product.vo.SpuFavVo;
import com.mall.product.vo.SpuSaveVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.product.entity.SpuInfoEntity;
import com.mall.product.service.SpuInfoService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.mall.product.dto.SpuInfoQueryDto;


/**
 * spu信息
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@Slf4j
@RestController
@RequestMapping("api/product/spuinfo")
@RequiredArgsConstructor
public class SpuInfoController {

    private final SpuInfoService spuInfoService;

    /**
     * 订单模块远程调用，根据skuId查询spu的信息
     */
    @GetMapping(value = "/skuId/{skuId}")
    public Result<Object> getSpuInfoBySkuId(@PathVariable Long skuId) {
        SpuInfoEntity spuInfoEntity = spuInfoService.getSpuInfoBySkuId(skuId);

        return Result.success().setData(spuInfoEntity);
    }

    /**
     * 订单模块远程调用：批量按 skuId 查询 spu 信息（品牌补全用，替代逐个调用）
     *
     * @param skuIds SKU id 列表
     * @return skuId -> spu 信息（无关联 spu 的 sku 不在结果中）
     */
    @PostMapping("/batchSpuInfoBySkuIds")
    public Result<Map<Long, SpuInfoEntity>> batchSpuInfoBySkuIds(@RequestBody List<Long> skuIds) {
        return Result.success(spuInfoService.getSpuInfoMapBySkuIds(skuIds));
    }

    @PostMapping("{spuId}/up")
    public Result<Object> spuUp(@PathVariable Long spuId) {
        return spuInfoService.up(spuId);
    }

    /**
     * 收藏列表聚合：批量查询 SPU 展示信息（会员端收藏页远程调用）
     *
     * @param spuIds SPU id 列表
     * @return spuId -> {名称/主图/首个SKU价格/分类名}
     */
    @PostMapping("/favInfo")
    public Result<Map<Long, SpuFavVo>> favInfo(@RequestBody List<Long> spuIds) {
        return Result.success(spuInfoService.favInfo(spuIds));
    }

    /**
     * 下架：删除 ES 中上架数据并更新状态
     */
    @PostMapping("{spuId}/down")
    public Result<Object> spuDown(@PathVariable Long spuId) {
        return spuInfoService.down(spuId);
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(SpuInfoQueryDto query) {
        PageUtils page = spuInfoService.queryPageByCondition(query.toMap());

        return Result.success(page);
    }

    /**
     * 信息
     */
    @GetMapping("/info/{id}")
    public Result<SpuInfoEntity> info(@PathVariable Long id) {
        SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return Result.success(spuInfo);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody SpuSaveVo vo) {
        try {
            spuInfoService.saveSpuInfo(vo);
        } catch (Exception e) {
            // 事务已回滚，返回失败原因，前端可见
            log.error("保存SPU失败", e);
            return Result.fail(500, "保存SPU失败：" + e.getMessage());
        }
        return Result.success();
    }

    /**
     * 批量发布商品：接收商品列表（图片直接传 URL），逐个保存并自动上架
     */
    @PostMapping("/batchPublish")
    public Result<Object> batchPublish(@RequestBody List<SpuSaveVo> vos) {
        try {
            return Result.success(spuInfoService.batchPublish(vos));
        } catch (Exception e) {
            log.error("批量发布SPU失败", e);
            return Result.fail(500, "批量发布失败：" + e.getMessage());
        }
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody SpuInfoEntity spuInfo) {
        spuInfoService.updateById(spuInfo);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] ids) {
        spuInfoService.removeByIds(Arrays.asList(ids));

        return Result.success();
    }

}
