package com.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.admin.entity.SysLogEntity;
import com.mall.admin.service.SysLogService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("sysLogService")
public class SysLogServiceImpl extends ServiceImpl<com.mall.admin.dao.SysLogDao, SysLogEntity> implements SysLogService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String username = (String) params.get("username");

        IPage<SysLogEntity> page = this.page(
                new Query<SysLogEntity>().getPage(params),
                new LambdaQueryWrapper<SysLogEntity>()
                        .like(StringUtils.isNotBlank(username), SysLogEntity::getUsername, username)
        );
        return new PageUtils(page);
    }
}
