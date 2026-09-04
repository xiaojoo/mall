package com.mall.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.admin.entity.SysUserTokenEntity;
import com.mall.admin.service.SysUserTokenService;
import com.mall.admin.utils.TokenGenerator;
import com.mall.common.utils.Result;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service("sysUserTokenService")
public class SysUserTokenServiceImpl extends ServiceImpl<com.mall.admin.dao.SysUserTokenDao, SysUserTokenEntity> implements SysUserTokenService {

    private static final int EXPIRE = 3600 * 12;

    @Override
    public Result<String> createToken(long userId) {
        String token = TokenGenerator.generateValue();
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + EXPIRE * 1000L);

        SysUserTokenEntity tokenEntity = this.getById(userId);
        if (tokenEntity == null) {
            tokenEntity = new SysUserTokenEntity();
            tokenEntity.setUserId(userId);
            tokenEntity.setToken(token);
            tokenEntity.setUpdateTime(now);
            tokenEntity.setExpireTime(expireTime);
            this.save(tokenEntity);
        } else {
            tokenEntity.setToken(token);
            tokenEntity.setUpdateTime(now);
            tokenEntity.setExpireTime(expireTime);
            this.updateById(tokenEntity);
        }
        return Result.success(token).putExtra("expire", EXPIRE);
    }

    @Override
    public void logout(long userId) {
        SysUserTokenEntity tokenEntity = new SysUserTokenEntity();
        tokenEntity.setUserId(userId);
        tokenEntity.setToken(TokenGenerator.generateValue());
        this.updateById(tokenEntity);
    }
}
