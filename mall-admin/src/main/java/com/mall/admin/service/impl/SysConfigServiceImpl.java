package com.mall.admin.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.admin.entity.SysConfigEntity;
import com.mall.admin.service.SysConfigService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Service("sysConfigService")
public class SysConfigServiceImpl extends ServiceImpl<com.mall.admin.dao.SysConfigDao, SysConfigEntity> implements SysConfigService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String paramKey = (String) params.get("paramKey");

        IPage<SysConfigEntity> page = this.page(
                new Query<SysConfigEntity>().getPage(params),
                new LambdaQueryWrapper<SysConfigEntity>()
                        .like(StringUtils.isNotBlank(paramKey), SysConfigEntity::getParamKey, paramKey)
        );
        return new PageUtils(page);
    }

    @Override
    public void saveConfig(SysConfigEntity config) {
        this.save(config);
    }

    @Override
    public void updateConfig(SysConfigEntity config) {
        this.updateById(config);
    }

    @Override
    public void deleteBatch(Long[] ids) {
        this.removeByIds(Arrays.asList(ids));
    }

    @Override
    public String getValue(String key) {
        SysConfigEntity config = this.getOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, key));
        return config != null ? config.getParamValue() : null;
    }

    @Override
    public <T> T getConfigObject(String key, Class<T> clazz) {
        String value = getValue(key);
        if (StringUtils.isNotBlank(value)) {
            return JSON.parseObject(value, clazz);
        }
        return null;
    }
}
