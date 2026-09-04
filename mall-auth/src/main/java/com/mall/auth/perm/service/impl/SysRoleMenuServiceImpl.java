package com.mall.auth.perm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.auth.perm.dao.SysRoleMenuDao;
import com.mall.auth.perm.entity.SysRoleMenuEntity;
import com.mall.auth.perm.service.SysRoleMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service("sysRoleMenuService")
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuDao, SysRoleMenuEntity> implements SysRoleMenuService {

    @Override
    public List<Long> queryMenuIdList(Long roleId) {
        return baseMapper.queryMenuIdList(roleId);
    }

    @Override
    @Transactional
    public void saveOrUpdate(Long roleId, List<Long> menuIdList) {
        // 先删除原有关联
        this.remove(new LambdaQueryWrapper<SysRoleMenuEntity>()
                .eq(SysRoleMenuEntity::getRoleId, roleId));

        // 保存新关联
        if (menuIdList != null && !menuIdList.isEmpty()) {
            // 去重，防止同批重复插入触发 uk_role_menu 唯一键冲突
            List<Long> distinct = menuIdList.stream().distinct().collect(Collectors.toList());
            List<SysRoleMenuEntity> entities = new ArrayList<>();
            for (Long menuId : distinct) {
                SysRoleMenuEntity entity = new SysRoleMenuEntity();
                entity.setRoleId(roleId);
                entity.setMenuId(menuId);
                entities.add(entity);
            }
            this.saveBatch(entities);
        }
    }
}
