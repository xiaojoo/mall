package com.mall.admin.service.impl;
import com.mall.admin.entity.SysUserEntity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.admin.entity.SysUserRoleEntity;
import com.mall.admin.service.SysUserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("sysUserRoleService")
public class SysUserRoleServiceImpl extends ServiceImpl<com.mall.admin.dao.SysUserRoleDao, SysUserRoleEntity> implements SysUserRoleService {

    @Override
    @Transactional
    public void saveOrUpdate(Long userId, List<Long> roleIdList) {
        // 先删除
        this.remove(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId));
        // 再保存
        if (roleIdList != null && !roleIdList.isEmpty()) {
            for (Long roleId : roleIdList) {
                SysUserRoleEntity entity = new SysUserRoleEntity();
                entity.setUserId(userId);
                entity.setRoleId(roleId);
                this.save(entity);
            }
        }
    }

    @Override
    public List<Long> queryRoleIdList(Long userId) {
        return this.baseMapper.selectList(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getUserId, userId))
                .stream().map(SysUserRoleEntity::getRoleId).toList();
    }
}
