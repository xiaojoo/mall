package com.mall.admin.service.impl;
import com.mall.admin.entity.SysMenuEntity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.admin.entity.SysRoleEntity;
import com.mall.admin.service.SysRoleMenuService;
import com.mall.admin.service.SysRoleService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service("sysRoleService")
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<com.mall.admin.dao.SysRoleDao, SysRoleEntity> implements SysRoleService {

    private final SysRoleMenuService sysRoleMenuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String roleName = (String) params.get("roleName");

        IPage<SysRoleEntity> page = this.page(
                new Query<SysRoleEntity>().getPage(params),
                new LambdaQueryWrapper<SysRoleEntity>()
                        .like(StringUtils.isNotBlank(roleName), SysRoleEntity::getRoleName, roleName)
        );
        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void saveRole(SysRoleEntity role) {
        role.setCreateTime(new Date());
        this.save(role);
        sysRoleMenuService.saveOrUpdate(role.getRoleId(), role.getMenuIdList());
    }

    @Override
    @Transactional
    public void updateRole(SysRoleEntity role) {
        this.updateById(role);
        sysRoleMenuService.saveOrUpdate(role.getRoleId(), role.getMenuIdList());
    }

    @Override
    @Transactional
    public void deleteBatch(Long[] roleIds) {
        this.removeByIds(Arrays.asList(roleIds));
    }

    @Override
    public List<Long> queryRoleIdList(Long createUserId) {
        // 注意：SysRoleEntity 没有 createUserId 字段，可能需要根据实际业务调整
        // 暂时返回空列表，或根据 deptId 查询
        return new ArrayList<>();
    }
}
