package com.mall.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.admin.entity.SysUserEntity;

import java.util.List;
import java.util.Map;

public interface SysUserService extends IService<SysUserEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<String> queryAllPerms(Long userId);

    List<Long> queryAllMenuId(Long userId);

    SysUserEntity queryByUserName(String username);

    void saveUser(SysUserEntity user);

    void updateUser(SysUserEntity user);

    void deleteBatch(Long[] userIds);

    boolean updatePassword(Long userId, String password, String newPassword);
}
