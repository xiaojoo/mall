package com.mall.auth.perm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.auth.perm.entity.SysMenuEntity;

import java.util.List;

public interface SysMenuService extends IService<SysMenuEntity> {

    /**
     * 根据父ID查询子菜单
     */
    List<SysMenuEntity> queryListParentId(Long parentId, List<Long> menuIdList);

    List<SysMenuEntity> queryListParentId(Long parentId);

    /**
     * 查询非按钮类型菜单
     */
    List<SysMenuEntity> queryNotButtonList();

    /**
     * 获取用户的菜单树
     */
    List<SysMenuEntity> getUserMenuList(Long userId);

    /**
     * 获取用户的所有权限标识（含按钮 type=2）
     */
    List<String> queryAllPerms(Long userId);

    /**
     * 删除菜单（同时删除角色-菜单关联）
     */
    void delete(Long menuId);
}
