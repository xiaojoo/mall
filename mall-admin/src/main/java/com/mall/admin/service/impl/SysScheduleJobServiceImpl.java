package com.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.admin.dao.SysScheduleJobDao;
import com.mall.admin.entity.SysScheduleJobEntity;
import com.mall.admin.service.SysScheduleJobService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SysScheduleJobServiceImpl extends ServiceImpl<SysScheduleJobDao, SysScheduleJobEntity> implements SysScheduleJobService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String jobName = (String) params.get("jobName");
        LambdaQueryWrapper<SysScheduleJobEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(jobName)) {
            wrapper.like(SysScheduleJobEntity::getJobName, jobName);
        }
        IPage<SysScheduleJobEntity> page = this.page(new Query<SysScheduleJobEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }
}
