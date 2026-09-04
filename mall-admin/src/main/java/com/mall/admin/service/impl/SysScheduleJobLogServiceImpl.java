package com.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.admin.dao.SysScheduleJobLogDao;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.admin.entity.SysScheduleJobLogEntity;
import com.mall.admin.service.SysScheduleJobLogService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Service
public class SysScheduleJobLogServiceImpl extends ServiceImpl<SysScheduleJobLogDao, SysScheduleJobLogEntity> implements SysScheduleJobLogService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String jobId = (String) params.get("jobId");
        IPage<SysScheduleJobLogEntity> page = this.page(
                new Query<SysScheduleJobLogEntity>().getPage(params),
                new LambdaQueryWrapper<SysScheduleJobLogEntity>()
                        .eq(StringUtils.isNotBlank(jobId), SysScheduleJobLogEntity::getJobId, jobId)
                        .orderByDesc(SysScheduleJobLogEntity::getCreateTime)
        );
        return new PageUtils(page);
    }
}
