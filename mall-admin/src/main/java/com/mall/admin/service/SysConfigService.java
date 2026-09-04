package com.mall.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.admin.entity.SysConfigEntity;

import java.util.Map;

public interface SysConfigService extends IService<SysConfigEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveConfig(SysConfigEntity config);

    void updateConfig(SysConfigEntity config);

    void deleteBatch(Long[] ids);

    String getValue(String key);

    <T> T getConfigObject(String key, Class<T> clazz);
}
