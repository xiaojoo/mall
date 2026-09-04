package com.mall.auth.perm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.auth.perm.dao.SysLogDao;
import com.mall.auth.perm.entity.SysLogEntity;
import com.mall.auth.perm.service.SysLogService;
import org.springframework.stereotype.Service;

@Service("sysLogService")
public class SysLogServiceImpl extends ServiceImpl<SysLogDao, SysLogEntity> implements SysLogService {
}
