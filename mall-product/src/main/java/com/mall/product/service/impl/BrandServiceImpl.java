package com.mall.product.service.impl;

import com.mall.product.service.CategoryBrandRelationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.dao.BrandDao;
import com.mall.product.entity.BrandEntity;
import com.mall.product.service.BrandService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;


@Slf4j
@Service("brandService")
@RequiredArgsConstructor
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {

    final CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        // 获取key
        String key = (String) params.get("key");
        LambdaQueryWrapper<BrandEntity> wrapper = new LambdaQueryWrapper<BrandEntity>();
        if (!StringUtils.isEmpty(key)) {
            wrapper.eq(BrandEntity::getBrandId, key).or().like(BrandEntity::getName, key);
        }
        IPage<BrandEntity> page = this.page(
                new Query<BrandEntity>().getPage(params), wrapper
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void updateDetail(BrandEntity brand) {
        // 保证冗余字段的数据一致
        this.updateById(brand);
        if (!StringUtils.isEmpty(brand.getName())) {
            // 同步更新其他关联表中的数据
            categoryBrandRelationService.updateBrand(brand.getBrandId(), brand.getName());
        }
    }

    @Cacheable(value = "brand", key = "'brandInfo:'+#root.args[0]")
    @Override
    public List<BrandEntity> getBrandsByIds(List<Long> brandIds) {
        return baseMapper.selectList(new LambdaQueryWrapper<BrandEntity>().in(BrandEntity::getBrandId, brandIds));
    }

    @Override
    public List<Map<String, Object>> batchSave(List<BrandEntity> brands) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (brands == null || brands.isEmpty()) {
            return results;
        }
        int index = 0;
        for (BrandEntity brand : brands) {
            index++;
            Map<String, Object> item = new HashMap<>();
            item.put("index", index);
            item.put("name", brand.getName());

            // 品牌名必填（List 参数不会触发分组校验，手动校验）
            if (StringUtils.isBlank(brand.getName())) {
                item.put("success", false);
                item.put("error", "品牌名不能为空");
                results.add(item);
                continue;
            }

            try {
                // 默认值兜底：显示状态/排序/首字母/介绍
                if (brand.getShowStatus() == null) {
                    brand.setShowStatus(1);
                }
                if (brand.getSort() == null) {
                    brand.setSort(0);
                }
                if (StringUtils.isNotBlank(brand.getFirstLetter())) {
                    brand.setFirstLetter(brand.getFirstLetter().trim().toUpperCase());
                }
                if (brand.getLogo() == null) {
                    brand.setLogo("");
                }
                if (brand.getDescript() == null) {
                    brand.setDescript("");
                }
                this.save(brand);
                item.put("success", true);
                item.put("brandId", brand.getBrandId());
            } catch (Exception e) {
                // 单条失败不影响其他条
                item.put("success", false);
                item.put("error", e.getMessage());
                log.error("批量添加品牌失败: {}", brand.getName(), e);
            }
            results.add(item);
        }
        return results;
    }

}