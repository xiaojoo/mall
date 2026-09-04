package com.mall.product.service.impl;

import com.mall.common.constant.ProductConstant;
import com.mall.product.entity.AttrEntity;
import com.mall.product.service.AttrService;
import com.mall.product.vo.AttrGroupWithAttrsVo;
import com.mall.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
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

import com.mall.product.dao.AttrGroupDao;
import com.mall.product.entity.AttrGroupEntity;
import com.mall.product.service.AttrGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;


@Slf4j
@Service("attrGroupService")
@RequiredArgsConstructor
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    final AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params), new LambdaQueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        LambdaQueryWrapper<AttrGroupEntity> wrapper = new LambdaQueryWrapper<AttrGroupEntity>();
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((obj) -> {
                obj.eq(AttrGroupEntity::getAttrGroupId, key).or().like(AttrGroupEntity::getAttrGroupName, key);
            });
        }

        if (catelogId == 0) {
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        } else {
            wrapper.eq(AttrGroupEntity::getCatelogId, catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        }
    }

    /**
     * 根据分类id查出所有的分组以及这些组里面的属性
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrByCatelogId(Long catelogId) {
        //1、查询分组信息
        List<AttrGroupEntity> attrGroupEntities = this.list(new LambdaQueryWrapper<AttrGroupEntity>().eq(AttrGroupEntity::getCatelogId, catelogId));

        //2、查询所有属性，规格参数只保留基本属性（attrType=1），排除销售属性（如颜色、屏幕分辨率）
        List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().map(group -> {
            AttrGroupWithAttrsVo attrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(group, attrsVo);
            List<AttrEntity> attrs = attrService.getReLationAttr(attrsVo.getAttrGroupId());
            if (attrs != null) {
                attrs = attrs.stream()
                        .filter(attr -> attr.getAttrType() != null
                                && attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode())
                        .collect(Collectors.toList());
            }
            attrsVo.setAttrs(attrs);
            return attrsVo;
        })
                // 过滤掉没有任何基本属性的空分组
                .filter(vo -> vo.getAttrs() != null && !vo.getAttrs().isEmpty())
                .collect(Collectors.toList());

        return collect;
    }

    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        AttrGroupDao baseMapper = this.getBaseMapper();
        return baseMapper.getAttrGroupWithAttrsBySpuId(spuId, catalogId);
    }

    @Override
    public List<Map<String, Object>> batchSave(List<AttrGroupEntity> groups) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (groups == null || groups.isEmpty()) {
            return results;
        }
        int index = 0;
        for (AttrGroupEntity group : groups) {
            index++;
            Map<String, Object> item = new HashMap<>();
            item.put("index", index);
            item.put("attrGroupName", group.getAttrGroupName());

            // 组名/所属分类必填（List 参数不会触发校验，手动校验）
            if (StringUtils.isBlank(group.getAttrGroupName())) {
                item.put("success", false);
                item.put("error", "组名不能为空");
                results.add(item);
                continue;
            }
            if (group.getCatelogId() == null) {
                item.put("success", false);
                item.put("error", "所属分类不能为空");
                results.add(item);
                continue;
            }

            try {
                if (group.getSort() == null) {
                    group.setSort(0);
                }
                if (group.getDescript() == null) {
                    group.setDescript("");
                }
                if (group.getIcon() == null) {
                    group.setIcon("");
                }
                this.save(group);
                item.put("success", true);
                item.put("attrGroupId", group.getAttrGroupId());
            } catch (Exception e) {
                // 单条失败不影响其他条
                item.put("success", false);
                item.put("error", e.getMessage());
                log.error("批量添加属性分组失败: {}", group.getAttrGroupName(), e);
            }
            results.add(item);
        }
        return results;
    }
}