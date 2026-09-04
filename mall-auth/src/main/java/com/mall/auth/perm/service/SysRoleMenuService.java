package com.mall.auth.perm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.auth.perm.entity.SysRoleMenuEntity;

import java.util.List;

public interface SysRoleMenuService extends IService<SysRoleMenuEntity> {

    /**
     * 查询角色的所有菜单ID
     */
    List<Long> queryMenuIdList(Long roleId);

    /**
     * 保存或更新角色的菜单关联
     */
    void saveOrUpdate(Long roleId, List<Long> menuIdList);
}
