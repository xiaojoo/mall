package com.mall.product.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.dao.ProductAttrValueDao;
import com.mall.product.entity.ProductAttrValueEntity;
import com.mall.product.service.ProductAttrValueService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;


@Service("productAttrValueService")
@RequiredArgsConstructor
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    final ProductAttrValueDao productAttrValueDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params), new LambdaQueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveProductAttr(List<ProductAttrValueEntity> productAttrValueEntity) {
        this.saveBatch(productAttrValueEntity);
    }

    @Override
    public List<ProductAttrValueEntity> baseAttrlistforspu(Long spuId) {
        return this.baseMapper.selectList(new LambdaQueryWrapper<ProductAttrValueEntity>().eq(ProductAttrValueEntity::getSpuId, spuId));
    }

    @Transactional
    @Override
    public void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities) {
        // 1、删除这个spuId之前对应的所有属性
        this.baseMapper.delete(new LambdaQueryWrapper<ProductAttrValueEntity>().eq(ProductAttrValueEntity::getSpuId, spuId));

        List<ProductAttrValueEntity> collect = entities.stream().map(item -> {
            item.setSpuId(spuId);
            return item;
        }).collect(Collectors.toList());
        this.saveBatch(collect);
    }

}