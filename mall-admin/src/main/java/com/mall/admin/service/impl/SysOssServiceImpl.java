package com.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.admin.entity.SysOssEntity;
import com.mall.admin.service.SysOssService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("sysOssService")
public class SysOssServiceImpl extends ServiceImpl<com.mall.admin.dao.SysOssDao, SysOssEntity> implements SysOssService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SysOssEntity> page = this.page(new Query<SysOssEntity>().getPage(params));
        return new PageUtils(page);
    }
}
