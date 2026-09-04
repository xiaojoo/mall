package com.mall.auth.perm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.auth.perm.entity.SysUserRoleEntity;

import java.util.List;

public interface SysUserRoleService extends IService<SysUserRoleEntity> {

    /**
     * 保存或更新用户的角色关联
     */
    void saveOrUpdate(Long userId, List<Long> roleIdList);
}
