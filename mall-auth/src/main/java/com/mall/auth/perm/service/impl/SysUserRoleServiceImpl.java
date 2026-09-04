package com.mall.auth.perm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.auth.perm.dao.SysUserRoleDao;
import com.mall.auth.perm.entity.SysUserRoleEntity;
import com.mall.auth.perm.service.SysUserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service("sysUserRoleService")
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleDao, SysUserRoleEntity> implements SysUserRoleService {

    @Override
    @Transactional
    public void saveOrUpdate(Long userId, List<Long> roleIdList) {
        this.remove(new LambdaQueryWrapper<SysUserRoleEntity>()
                .eq(SysUserRoleEntity::getUserId, userId));

        if (roleIdList != null && !roleIdList.isEmpty()) {
            // 去重，防止同批重复插入触发 uk_user_role 唯一键冲突
            List<Long> distinct = roleIdList.stream().distinct().collect(Collectors.toList());
            List<SysUserRoleEntity> entities = new ArrayList<>();
            for (Long roleId : distinct) {
                SysUserRoleEntity entity = new SysUserRoleEntity();
                entity.setUserId(userId);
                entity.setRoleId(roleId);
                entities.add(entity);
            }
            this.saveBatch(entities);
        }
    }
}
