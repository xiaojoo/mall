package com.mall.auth.perm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.auth.perm.dao.SysUserDao;
import com.mall.auth.perm.entity.SysMenuEntity;
import com.mall.auth.perm.entity.SysUserEntity;
import com.mall.auth.perm.entity.SysUserRoleEntity;
import com.mall.auth.perm.service.SysMenuService;
import com.mall.auth.perm.service.SysUserRoleService;
import com.mall.auth.perm.service.SysUserService;
import com.mall.common.exception.RRException;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service("sysUserService")
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserDao, SysUserEntity> implements SysUserService {

    private final SysUserRoleService sysUserRoleService;
    private final SysMenuService sysMenuService;

    private static final long SUPER_ADMIN = 1L;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String username = (String) params.get("username");

        IPage<SysUserEntity> page = this.page(
                new Query<SysUserEntity>().getPage(params),
                new LambdaQueryWrapper<SysUserEntity>()
                        .like(StringUtils.isNotBlank(username), SysUserEntity::getUsername, username)
                        .orderByDesc(SysUserEntity::getCreateTime)
        );
        return new PageUtils(page);
    }

    @Override
    public SysUserEntity queryByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username));
    }

    @Override
    @Transactional
    public void createUser(SysUserEntity user) {
        SysUserEntity existUser = queryByUsername(user.getUsername());
        if (existUser != null) {
            throw new RRException("用户名已存在");
        }
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setStatus(1);
        this.save(user);

        if (user.getRoleIdList() != null && !user.getRoleIdList().isEmpty()) {
            sysUserRoleService.saveOrUpdate(user.getUserId(), toLongList(user.getRoleIdList()));
        }
    }

    @Override
    @Transactional
    public void updateUser(SysUserEntity user) {
        user.setUpdateTime(new Date());
        user.setPassword(null);
        this.updateById(user);

        if (user.getRoleIdList() != null) {
            sysUserRoleService.saveOrUpdate(user.getUserId(), toLongList(user.getRoleIdList()));
        }
    }

    @Override
    @Transactional
    public void deleteBatch(Long[] userIds) {
        for (Long userId : userIds) {
            if (userId == SUPER_ADMIN) {
                throw new RRException("不能删除超级管理员");
            }
        }
        this.removeByIds(Arrays.asList(userIds));
        for (Long userId : userIds) {
            sysUserRoleService.remove(new LambdaQueryWrapper<SysUserRoleEntity>()
                    .eq(SysUserRoleEntity::getUserId, userId));
        }
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        SysUserEntity user = this.getById(userId);
        if (user == null) {
            throw new RRException("用户不存在");
        }
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        user.setUpdateTime(new Date());
        this.updateById(user);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUserEntity user = this.getById(userId);
        if (user == null) {
            throw new RRException("用户不存在");
        }
        if (!new BCryptPasswordEncoder().matches(oldPassword, user.getPassword())) {
            throw new RRException("原密码错误");
        }
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        user.setUpdateTime(new Date());
        this.updateById(user);
    }

    private List<Long> toLongList(List<String> ids) {
        if (ids == null) {
            return null;
        }
        return ids.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public List<Long> queryRoleIdList(Long userId) {
        return baseMapper.queryRoleIdList(userId);
    }

    @Override
    public List<String> queryPermsList(Long userId) {
        if (userId == SUPER_ADMIN) {
            return Collections.singletonList("*:*:*");
        }
        // 直接查全部权限标识（含按钮 type=2，SQL 已去重去空）
        return sysMenuService.queryAllPerms(userId);
    }
}
