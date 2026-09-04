package com.mall.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.admin.entity.SysScheduleJobEntity;
import com.mall.common.utils.PageUtils;
import java.util.Map;

public interface SysScheduleJobService extends IService<SysScheduleJobEntity> {
    PageUtils queryPage(Map<String, Object> params);
}
