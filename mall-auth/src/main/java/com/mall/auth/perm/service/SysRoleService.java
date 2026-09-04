package com.mall.auth.perm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.auth.perm.entity.SysRoleEntity;
import com.mall.common.utils.PageUtils;

import java.util.List;
import java.util.Map;

public interface SysRoleService extends IService<SysRoleEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 创建角色（含菜单绑定）
     */
    void createRole(SysRoleEntity role);

    /**
     * 修改角色（含菜单绑定）
     */
    void updateRole(SysRoleEntity role);

    /**
     * 批量删除角色
     */
    void deleteBatch(Long[] roleIds);

    /**
     * 查询角色的所有菜单ID
     */
    List<Long> queryMenuIdList(Long roleId);
}
