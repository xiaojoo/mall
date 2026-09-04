package com.mall.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.admin.entity.SysRoleEntity;

import java.util.List;
import java.util.Map;

public interface SysRoleService extends IService<SysRoleEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveRole(SysRoleEntity role);

    void updateRole(SysRoleEntity role);

    void deleteBatch(Long[] roleIds);

    List<Long> queryRoleIdList(Long createUserId);
}
