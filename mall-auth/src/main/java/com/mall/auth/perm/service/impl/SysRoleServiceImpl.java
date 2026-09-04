package com.mall.auth.perm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.auth.perm.dao.SysRoleDao;
import com.mall.auth.perm.entity.SysRoleEntity;
import com.mall.auth.perm.entity.SysRoleMenuEntity;
import com.mall.auth.perm.service.SysRoleMenuService;
import com.mall.auth.perm.service.SysRoleService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service("sysRoleService")
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleDao, SysRoleEntity> implements SysRoleService {

    private final SysRoleMenuService sysRoleMenuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String roleName = (String) params.get("roleName");

        IPage<SysRoleEntity> page = this.page(
                new Query<SysRoleEntity>().getPage(params),
                new LambdaQueryWrapper<SysRoleEntity>()
                        .like(StringUtils.isNotBlank(roleName), SysRoleEntity::getRoleName, roleName)
                        .orderByDesc(SysRoleEntity::getCreateTime)
        );
        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void createRole(SysRoleEntity role) {
        role.setCreateTime(new Date());
        role.setUpdateTime(new Date());
        role.setStatus(1);
        // 前端表单无 role_code，自动生成唯一编码（role_code 有唯一索引）
        if (StringUtils.isBlank(role.getRoleCode())) {
            role.setRoleCode("ROLE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }
        this.save(role);
        sysRoleMenuService.saveOrUpdate(role.getRoleId(), toLongList(role.getMenuIdList()));
    }

    @Override
    @Transactional
    public void updateRole(SysRoleEntity role) {
        role.setUpdateTime(new Date());
        this.updateById(role);
        sysRoleMenuService.saveOrUpdate(role.getRoleId(), toLongList(role.getMenuIdList()));
    }

    @Override
    @Transactional
    public void deleteBatch(Long[] roleIds) {
        this.removeByIds(Arrays.asList(roleIds));
        for (Long roleId : roleIds) {
            sysRoleMenuService.remove(new LambdaQueryWrapper<SysRoleMenuEntity>()
                    .eq(SysRoleMenuEntity::getRoleId, roleId));
        }
    }

    private List<Long> toLongList(List<String> ids) {
        if (ids == null) {
            return null;
        }
        return ids.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    @Override
    public List<Long> queryMenuIdList(Long roleId) {
        return sysRoleMenuService.queryMenuIdList(roleId);
    }
}
