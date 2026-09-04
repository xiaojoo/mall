package com.mall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mall.product.entity.ProductAttrValueEntity;
import com.mall.product.service.ProductAttrValueService;
import com.mall.product.vo.AttrRespVo;
import com.mall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.product.service.AttrService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.mall.product.dto.AttrQueryDto;

/**
 * 商品属性
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@Slf4j
@RestController
@RequestMapping("api/product/attr")
@RequiredArgsConstructor
public class AttrController {

    private final AttrService attrService;


    final ProductAttrValueService productAttrValueService;

    @PostMapping("/update/{spuId}")
    public Result<Object> updateSpuAttr(@PathVariable Long spuId,
                           @RequestBody List<ProductAttrValueEntity> entities) {

        productAttrValueService.updateSpuAttr(spuId, entities);
        return Result.success();
    }

    // /product/attr/base/listforspu/{spuId}
    @GetMapping("/base/listforspu/{spuId}")
    public Result<Object> baseAttrlistforspu(@PathVariable Long spuId) {

        List<ProductAttrValueEntity> entities = productAttrValueService.baseAttrlistforspu(spuId);
        return Result.success(entities);
    }

    //product/attr/sale/list/0?
    ///product/attr/base/list/{catelogId}
    @GetMapping("/{attrType}/list/{catelogId}")
    public Result<Object> baseAttrList(AttrQueryDto query,
                          @PathVariable Long catelogId,
                          @PathVariable("attrType") String type) {
        PageUtils page = attrService.queryBaseAttrPage(query.toMap(), catelogId, type);
        return Result.success(page);
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    public Result<PageUtils> list(AttrQueryDto query) {
        PageUtils page = attrService.queryPage(query.toMap());

        return Result.success(page);
    }


    /**
     * search远程调用
     * 信息
     */
    @GetMapping("/info/{attrId}")
    public Result<AttrRespVo> info(@PathVariable Long attrId) {
//		AttrEntity attr = attrService.getById(attrId);
        AttrRespVo respVo = attrService.getAttrInfo(attrId);

        return Result.success(respVo);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody AttrVo attr) {
        attrService.saveAttr(attr);

        return Result.success();
    }

    /**
     * 批量添加（单条失败不影响其他条，返回每条结果）
     */
    @PostMapping("/batchSave")
    public Result<Object> batchSave(@RequestBody List<AttrVo> attrs) {
        try {
            return Result.success(attrService.batchSave(attrs));
        } catch (Exception e) {
            log.error("批量添加规则参数失败", e);
            return Result.fail(500, "批量添加失败：" + e.getMessage());
        }
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody AttrVo attr) {
        attrService.updateAttr(attr);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] attrIds) {
        attrService.removeByIds(Arrays.asList(attrIds));

        return Result.success();
    }

}
