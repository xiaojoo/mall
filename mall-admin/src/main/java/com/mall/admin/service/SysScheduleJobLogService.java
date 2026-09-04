package com.mall.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.admin.entity.SysScheduleJobLogEntity;
import com.mall.common.utils.PageUtils;
import java.util.Map;

public interface SysScheduleJobLogService extends IService<SysScheduleJobLogEntity> {
    PageUtils queryPage(Map<String, Object> params);
}
