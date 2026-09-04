package com.mall.admin.service.impl;
import com.mall.admin.entity.SysRoleEntity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.admin.entity.SysRoleMenuEntity;
import com.mall.admin.service.SysRoleMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("sysRoleMenuService")
public class SysRoleMenuServiceImpl extends ServiceImpl<com.mall.admin.dao.SysRoleMenuDao, SysRoleMenuEntity> implements SysRoleMenuService {

    @Override
    @Transactional
    public void saveOrUpdate(Long roleId, List<Long> menuIdList) {
        this.remove(new LambdaQueryWrapper<SysRoleMenuEntity>().eq(SysRoleMenuEntity::getRoleId, roleId));
        if (menuIdList != null && !menuIdList.isEmpty()) {
            for (Long menuId : menuIdList) {
                SysRoleMenuEntity entity = new SysRoleMenuEntity();
                entity.setRoleId(roleId);
                entity.setMenuId(menuId);
                this.save(entity);
            }
        }
    }

    @Override
    public List<Long> queryMenuIdList(Long roleId) {
        return this.baseMapper.selectList(new LambdaQueryWrapper<SysRoleMenuEntity>().eq(SysRoleMenuEntity::getRoleId, roleId))
                .stream().map(SysRoleMenuEntity::getMenuId).toList();
    }
}
