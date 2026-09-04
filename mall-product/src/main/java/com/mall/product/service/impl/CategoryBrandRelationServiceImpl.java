package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mall.common.exception.RRException;
import com.mall.product.dao.BrandDao;
import com.mall.product.dao.CategoryDao;
import com.mall.product.entity.BrandEntity;
import com.mall.product.entity.CategoryEntity;
import com.mall.product.service.BrandService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.dao.CategoryBrandRelationDao;
import com.mall.product.entity.CategoryBrandRelationEntity;
import com.mall.product.service.CategoryBrandRelationService;
import lombok.RequiredArgsConstructor;


@Service("categoryBrandRelationService")
@RequiredArgsConstructor
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    final BrandDao brandDao;


    final CategoryDao categoryDao;


    final CategoryBrandRelationDao relationDao;

    @Lazy
    private final BrandService brandService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params), new LambdaQueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();
        if (brandId == null || catelogId == null) {
            throw new RRException("品牌和分类不能为空");
        }
        // 查出详情名字
        BrandEntity brandEntity = brandDao.selectById(brandId);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (brandEntity == null) {
            throw new RRException("品牌不存在：" + brandId);
        }
        if (categoryEntity == null) {
            throw new RRException("分类不存在：" + catelogId);
        }
        categoryBrandRelation.setBrandName(brandEntity.getName());
        categoryBrandRelation.setCatelogName(categoryEntity.getName());
        this.save(categoryBrandRelation);
    }

    @Override
    public void updateBrand(Long brandId, String name) {
        CategoryBrandRelationEntity categoryBrandRelationEntity = new CategoryBrandRelationEntity();
        categoryBrandRelationEntity.setBrandId(brandId);
        categoryBrandRelationEntity.setBrandName(name);
        this.update(categoryBrandRelationEntity, new LambdaUpdateWrapper<CategoryBrandRelationEntity>()
                .eq(CategoryBrandRelationEntity::getBrandId, brandId));
    }

    @Override
    public void updateCategory(Long catId, String name) {
        this.baseMapper.updateCategory(catId, name);
    }

    @Override
    public List<BrandEntity> getBrandsByCatId(Long catId) {
        List<CategoryBrandRelationEntity> catelogId = relationDao.selectList(new LambdaQueryWrapper<CategoryBrandRelationEntity>()
                .eq(CategoryBrandRelationEntity::getCatelogId, catId));
        return catelogId.stream()
                // 过滤品牌已被删除的孤儿关联数据，避免返回 null 元素
                .map(item -> brandService.getById(item.getBrandId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}