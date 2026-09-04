package com.mall.auth.perm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.auth.perm.entity.SysUserEntity;
import com.mall.common.utils.PageUtils;

import java.util.List;
import java.util.Map;

public interface SysUserService extends IService<SysUserEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 根据用户名查询
     */
    SysUserEntity queryByUsername(String username);

    /**
     * 创建用户
     */
    void createUser(SysUserEntity user);

    /**
     * 修改用户
     */
    void updateUser(SysUserEntity user);

    /**
     * 批量删除用户
     */
    void deleteBatch(Long[] userIds);

    /**
     * 重置密码
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 修改密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 查询用户的所有角色ID
     */
    List<Long> queryRoleIdList(Long userId);

    /**
     * 查询用户的权限标识列表
     */
    List<String> queryPermsList(Long userId);
}
