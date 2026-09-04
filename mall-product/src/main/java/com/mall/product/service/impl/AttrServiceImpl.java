package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mall.common.constant.ProductConstant;
import com.mall.product.dao.AttrAttrgroupRelationDao;
import com.mall.product.dao.AttrGroupDao;
import com.mall.product.dao.CategoryDao;
import com.mall.product.entity.AttrAttrgroupRelationEntity;
import com.mall.product.entity.AttrGroupEntity;
import com.mall.product.entity.CategoryEntity;
import com.mall.product.service.CategoryService;
import com.mall.product.vo.AttrGroupRelationVo;
import com.mall.product.vo.AttrRespVo;
import com.mall.product.vo.AttrVo;
import org.springframework.beans.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.dao.AttrDao;
import com.mall.product.entity.AttrEntity;
import com.mall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;


@Slf4j
@Service("attrService")
@RequiredArgsConstructor
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    final AttrAttrgroupRelationDao relationDao;


    final AttrGroupDao attrGroupDao;


    final CategoryDao categoryDao;


    final CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new LambdaQueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public Long saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        // 保存基本数据
        this.save(attrEntity);
        // 保存关联关系
        if (ProductConstant.AttrEnum.isBaseOrSale(attr.getAttrType()) && attr.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            relationDao.insert(relationEntity);
        }
        // 回填生成的 id（批量添加等场景需要拿到新 id）
        attr.setAttrId(attrEntity.getAttrId());
        return attrEntity.getAttrId();
    }

    @Override
    public List<Map<String, Object>> batchSave(List<AttrVo> attrs) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (attrs == null || attrs.isEmpty()) {
            return results;
        }
        int index = 0;
        for (AttrVo attr : attrs) {
            index++;
            Map<String, Object> item = new HashMap<>();
            item.put("index", index);
            item.put("attrName", attr.getAttrName());

            // 属性名/所属分类必填（List 参数不会触发校验，手动校验）
            if (StringUtils.isBlank(attr.getAttrName())) {
                item.put("success", false);
                item.put("error", "属性名不能为空");
                results.add(item);
                continue;
            }
            if (attr.getCatelogId() == null) {
                item.put("success", false);
                item.put("error", "所属分类不能为空");
                results.add(item);
                continue;
            }

            try {
                // 默认值兜底
                if (attr.getAttrType() == null) {
                    attr.setAttrType(1);
                }
                if (attr.getEnable() == null) {
                    attr.setEnable(1L);
                }
                if (attr.getSearchType() == null) {
                    attr.setSearchType(0);
                }
                if (attr.getShowDesc() == null) {
                    attr.setShowDesc(0);
                }
                if (attr.getValueType() == null) {
                    attr.setValueType(1);
                }
                if (attr.getValueSelect() == null) {
                    attr.setValueSelect("");
                }
                if (attr.getIcon() == null) {
                    attr.setIcon("");
                }
                Long attrId = saveAttr(attr);
                item.put("success", true);
                item.put("attrId", attrId);
            } catch (Exception e) {
                // 单条失败不影响其他条
                item.put("success", false);
                item.put("error", e.getMessage());
                log.error("批量添加规则参数失败: {}", attr.getAttrName(), e);
            }
            results.add(item);
        }
        return results;
    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType) {
        LambdaQueryWrapper<AttrEntity> queryWrapper = new LambdaQueryWrapper<AttrEntity>()
                .eq(AttrEntity::getAttrType, "base".equalsIgnoreCase(attrType) ? ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()
                        : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        if (catelogId != 0) {
            queryWrapper.eq(AttrEntity::getCatelogId, catelogId);
        }
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq(AttrEntity::getAttrId, key).or().like(AttrEntity::getAttrName, key);
            });
        }
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );
        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> respVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);

            // 设置分类何分组的名字
            if ("base".equalsIgnoreCase(attrType)) {
                AttrAttrgroupRelationEntity attrId = relationDao.selectOne(new LambdaQueryWrapper<AttrAttrgroupRelationEntity>()
                        .eq(AttrAttrgroupRelationEntity::getAttrId, attrEntity.getAttrId()));
                if (attrId != null && attrId.getAttrGroupId() != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrId.getAttrGroupId());
                    // 分组可能已被删除（孤儿关联），避免 NPE
                    if (attrGroupEntity != null) {
                        attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                    }
                }
            }
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());
        pageUtils.setList(respVos);
        return pageUtils;
    }

    @Cacheable(value = "attr", key = "'attrInfo:'+#root.args[0]")
    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo respVo = new AttrRespVo();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, respVo);

        if (ProductConstant.AttrEnum.isBaseOrSale(attrEntity.getAttrType())) {
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = relationDao.selectOne(
                    new LambdaQueryWrapper<AttrAttrgroupRelationEntity>().eq(AttrAttrgroupRelationEntity::getAttrId, attrId));
            // 设置分组信息
            if (attrAttrgroupRelationEntity != null) {
                respVo.setAttrGroupId(attrAttrgroupRelationEntity.getAttrGroupId());
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrAttrgroupRelationEntity.getAttrGroupId());
                if (attrGroupEntity != null) {
                    respVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }

        // 设置分类信息
        Long catelogId = attrEntity.getCatelogId();
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        respVo.setCatelogPath(catelogPath);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (categoryEntity != null) {
            respVo.setCatelogName(categoryEntity.getName());
        }
        return respVo;
    }

    @CacheEvict(value = "attr", allEntries = true)
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);
        // 维护分组关联（不限于基本属性，销售属性也可关联分组，如颜色/屏幕分组）
        if (attr.getAttrId() != null) {
            if (attr.getAttrGroupId() != null) {
                // 修改/新增分组关联
                AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
                relationEntity.setAttrGroupId(attr.getAttrGroupId());
                relationEntity.setAttrId(attr.getAttrId());
                Long count = relationDao.selectCount(new LambdaQueryWrapper<AttrAttrgroupRelationEntity>().eq(AttrAttrgroupRelationEntity::getAttrId, attr.getAttrId()));
                if (count > 0) {
                    relationDao.update(relationEntity, new LambdaUpdateWrapper<AttrAttrgroupRelationEntity>().eq(AttrAttrgroupRelationEntity::getAttrId, attr.getAttrId()));
                } else {
                    relationDao.insert(relationEntity);
                }
            } else {
                // 未选分组：清理旧关联，避免残留重复/脏数据
                relationDao.delete(new LambdaQueryWrapper<AttrAttrgroupRelationEntity>()
                        .eq(AttrAttrgroupRelationEntity::getAttrId, attr.getAttrId()));
            }
        }
    }

    /**
     * 根据分组id查找关联的所有基本信息
     */
    @Override
    public List<AttrEntity> getReLationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> entities = relationDao.selectList(new LambdaQueryWrapper<AttrAttrgroupRelationEntity>()
                .eq(AttrAttrgroupRelationEntity::getAttrGroupId, attrgroupId));
        List<Long> attrIds = entities.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());
        if (attrIds.isEmpty()) {
            return new ArrayList<>();
        }
        return this.listByIds(attrIds);
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {
        List<AttrAttrgroupRelationEntity> entities = Arrays.asList(vos).stream().map((item) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        relationDao.deleteBatchRelation(entities);
    }

    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        // 1、当前分组只能关联自己所属分类里面的所有属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();
        // 2、当前只能关联别的分组没有引用的属性
        // 2.1、当前分类下的其他分组
        List<AttrGroupEntity> group = attrGroupDao.selectList(new LambdaQueryWrapper<AttrGroupEntity>().eq(AttrGroupEntity::getCatelogId, catelogId));
        List<Long> collect = group.stream().map(AttrGroupEntity::getAttrGroupId).collect(Collectors.toList());

        // 2.2、分组关联的属性
        List<AttrAttrgroupRelationEntity> groupId = relationDao.selectList(new LambdaQueryWrapper<AttrAttrgroupRelationEntity>()
                .in(AttrAttrgroupRelationEntity::getAttrGroupId, collect));
        List<Long> attrIds = groupId.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());

        // 2.3、从当前分类的所有属性中移除这些属性
        LambdaQueryWrapper<AttrEntity> wrapper = new LambdaQueryWrapper<AttrEntity>().eq(AttrEntity::getCatelogId, catelogId)
                .eq(AttrEntity::getAttrType, ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if (!attrIds.isEmpty()) {
            wrapper.notIn(AttrEntity::getAttrId, attrIds);
        }
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((w) -> {
                w.eq(AttrEntity::getAttrId, key).or().like(AttrEntity::getAttrName, key);
            });
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    /**
     * 在指定的所有属性集合里面，挑出检索属性
     */
    @Override
    public List<Long> selectSearchAttrIds(List<Long> attrIds) {
        return baseMapper.selectSearchAttrIds(attrIds);
    }

}