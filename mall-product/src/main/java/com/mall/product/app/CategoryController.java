package com.mall.product.app;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mall.product.entity.CategoryEntity;
import com.mall.product.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.utils.Result;
import com.mall.common.utils.PageUtils;
import lombok.RequiredArgsConstructor;


/**
 * 商品三级分类
 *
 * @author sunxiaojie
 * @date 2024-08-01 12:44:34
 */
@RestController
@RequestMapping("api/product/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 列表：查询所有分类以及子分类，以树形结构组装
     */
    @GetMapping("/list/tree")
    public Result<List<CategoryEntity>> list() {
        List<CategoryEntity> entities = categoryService.listWithTree();

        return Result.success(entities);
    }

    /**
     * 按父级分类查询子分类（树形懒加载用）
     */
    @GetMapping("/list/{parentCid}")
    public Result<List<CategoryEntity>> listByParent(@PathVariable Long parentCid) {
        List<CategoryEntity> list = categoryService.list(
                new LambdaQueryWrapper<CategoryEntity>().eq(CategoryEntity::getParentCid, parentCid));
        return Result.success(list);
    }

    /**
     * 信息
     */
    @GetMapping("/info/{catId}")
    public Result<CategoryEntity> info(@PathVariable Long catId) {
        CategoryEntity category = categoryService.getById(catId);

        return Result.success(category);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody CategoryEntity category) {
        categoryService.save(category);

        return Result.success();
    }

    /**
     * 批量修改排序
     */
    @PostMapping("/update/sort")
    public Result<Object> updateSort(@RequestBody CategoryEntity[] category) {
        categoryService.updateBatchById(Arrays.asList(category));

        return Result.success();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody CategoryEntity category) {
//        categoryService.updateById(category);
        categoryService.updateCascade(category);

        return Result.success();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] catIds) {
        categoryService.removeByIds(Arrays.asList(catIds));

        return Result.success();
    }

}
