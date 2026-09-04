package com.mall.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.Result;
import com.mall.admin.entity.SysUserTokenEntity;

public interface SysUserTokenService extends IService<SysUserTokenEntity> {

    Result<String> createToken(long userId);

    void logout(long userId);
}
