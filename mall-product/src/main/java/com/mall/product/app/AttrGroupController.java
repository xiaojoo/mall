package com.mall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.product.entity.AttrAttrgroupRelationEntity;
import com.mall.product.entity.AttrEntity;
import com.mall.product.service.AttrAttrgroupRelationService;
import com.mall.product.service.AttrService;
import com.mall.product.service.impl.CategoryServiceImpl;
import com.mall.product.vo.AttrGroupRelationVo;
import com.mall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.mall.product.entity.AttrGroupEntity;
import com.mall.product.service.AttrGroupService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.mall.product.dto.AttrGroupQueryDto;


/**
 * 属性分组
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@Slf4j
@RestController
@RequestMapping("api/product/attrgroup")
@RequiredArgsConstructor
public class AttrGroupController {

    private final AttrGroupService attrGroupService;


    private final CategoryServiceImpl categoryService;


    private final AttrService attrService;


    final AttrAttrgroupRelationService relationService;

    /**
     * 获取销售属性
     */
    @GetMapping("/{catelogId}/withattr")
    public Result<Object> getAttrGroupWithAttrs(@PathVariable Long catelogId) {
        // 查询当前分类下的所有分组
        // 查出每个属性分组的所有属性
        List<AttrGroupWithAttrsVo> vos = attrGroupService.getAttrGroupWithAttrByCatelogId(catelogId);
        return Result.success(vos);
    }

    /**
     * 添加关联分类
     */
    @PostMapping("/attr/relation")
    public Result<Object> addRelation(@RequestBody List<AttrGroupRelationVo> vos) {
        // 幂等保存：先清理该属性已存在的关联，避免重复关联导致查询 selectOne 报 TooManyResultsException
        for (AttrGroupRelationVo vo : vos) {
            relationService.remove(new LambdaQueryWrapper<AttrAttrgroupRelationEntity>()
                    .eq(AttrAttrgroupRelationEntity::getAttrId, vo.getAttrId()));
        }
        relationService.saveBatch(vos);
        return Result.success();
    }

    /**
     * 获取分类关联列表
     */
    @GetMapping("/{attrgroupId}/attr/relation")
    public Result<Object> attrRelation(@PathVariable Long attrgroupId) {
        List<AttrEntity> entities = attrService.getReLationAttr(attrgroupId);

        return Result.success(entities);
    }

    /**
     * 获取分类没有关联列表
     */
    @GetMapping("/{attrgroupId}/noattr/relation")
    public Result<Object> attrNoRelation(@PathVariable Long attrgroupId,
                            AttrGroupQueryDto query) {
        PageUtils page = attrService.getNoRelationAttr(query.toMap(), attrgroupId);
        return Result.success(page);
    }

    /**
     * 删除分类关联
     */
    @PostMapping("/attr/relation/delete")
    public Result<Object> deleteRelation(@RequestBody AttrGroupRelationVo[] vos) {
        attrService.deleteRelation(vos);
        return Result.success();
    }

    /**
     * 列表（支持按 catelogId 过滤）
     */
    @GetMapping("/list")
    public Result<PageUtils> list(AttrGroupQueryDto query) {
        Long catelogId = query.parseCatelogId();
        PageUtils page = (catelogId == null)
                ? attrGroupService.queryPage(query.toMap())
                : attrGroupService.queryPage(query.toMap(), catelogId);
        return Result.success(page);
    }

    /**
     * 根据分类id查询属性分组列表（后台管理-属性分组下拉）
     */
    @GetMapping("/list/{catelogId}")
    public Result<PageUtils> listByCatelog(@PathVariable Long catelogId, AttrGroupQueryDto query) {
        PageUtils page = attrGroupService.queryPage(query.toMap(), catelogId);
        return Result.success(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{attrGroupId}")
    public Result<AttrGroupEntity> info(@PathVariable Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        Long catelogId = attrGroup.getCatelogId();
        Long[] path = categoryService.findCatelogPath(catelogId);
        attrGroup.setCatelogPath(path);
        return Result.success(attrGroup);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return Result.success();
    }

    /**
     * 批量添加（单条失败不影响其他条，返回每条结果）
     */
    @PostMapping("/batchSave")
    public Result<Object> batchSave(@RequestBody List<AttrGroupEntity> groups) {
        try {
            return Result.success(attrGroupService.batchSave(groups));
        } catch (Exception e) {
            log.error("批量添加属性分组失败", e);
            return Result.fail(500, "批量添加失败：" + e.getMessage());
        }
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return Result.success();
    }

}
