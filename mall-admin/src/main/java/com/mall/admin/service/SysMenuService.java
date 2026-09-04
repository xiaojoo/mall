package com.mall.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.admin.entity.SysMenuEntity;

import java.util.List;

public interface SysMenuService extends IService<SysMenuEntity> {

    List<SysMenuEntity> queryListParentId(Long parentId, List<Long> menuIdList);

    List<SysMenuEntity> queryListParentId(Long parentId);

    List<SysMenuEntity> queryNotButtonList();

    List<SysMenuEntity> getUserMenuList(Long userId);

    void delete(Long menuId);
}
