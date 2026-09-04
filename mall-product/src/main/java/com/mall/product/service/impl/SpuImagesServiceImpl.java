package com.mall.product.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.dao.SpuImagesDao;
import com.mall.product.entity.SpuImagesEntity;
import com.mall.product.service.SpuImagesService;
import org.springframework.transaction.annotation.Transactional;


@Service("spuImagesService")
public class SpuImagesServiceImpl extends ServiceImpl<SpuImagesDao, SpuImagesEntity> implements SpuImagesService {
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuImagesEntity> page = this.page(
                new Query<SpuImagesEntity>().getPage(params),
                new LambdaQueryWrapper<SpuImagesEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveImages(Long id, List<String> images) {
        if (images != null && !images.isEmpty()) {
            List<SpuImagesEntity> imagesEntity = images.stream().map(img -> {
                SpuImagesEntity spuImagesEntity = new SpuImagesEntity();
                spuImagesEntity.setSpuId(id);
                spuImagesEntity.setImgUrl(img);
                return spuImagesEntity;
            }).collect(Collectors.toList());
            this.saveBatch(imagesEntity);
        }
    }

}