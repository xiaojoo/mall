package com.mall.admin.service.impl;
import com.mall.admin.entity.SysRoleMenuEntity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.admin.entity.SysMenuEntity;
import com.mall.admin.service.SysMenuService;
import com.mall.admin.service.SysRoleMenuService;
import com.mall.admin.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("sysMenuService")
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<com.mall.admin.dao.SysMenuDao, SysMenuEntity> implements SysMenuService {

    private final SysUserService sysUserService;
    private final SysRoleMenuService sysRoleMenuService;
    private static final long SUPER_ADMIN = 1L;

    @Override
    public List<SysMenuEntity> queryListParentId(Long parentId, List<Long> menuIdList) {
        List<SysMenuEntity> menuList = queryListParentId(parentId);
        if (menuIdList == null) {
            return menuList;
        }
        List<SysMenuEntity> userMenuList = new ArrayList<>();
        for (SysMenuEntity menu : menuList) {
            if (menuIdList.contains(menu.getMenuId())) {
                userMenuList.add(menu);
            }
        }
        return userMenuList;
    }

    @Override
    public List<SysMenuEntity> queryListParentId(Long parentId) {
        return baseMapper.queryListParentId(parentId);
    }

    @Override
    public List<SysMenuEntity> queryNotButtonList() {
        return baseMapper.queryNotButtonList();
    }

    @Override
    public List<SysMenuEntity> getUserMenuList(Long userId) {
        if (userId == SUPER_ADMIN) {
            return getMenuList(null);
        }
        List<Long> menuIdList = sysUserService.queryAllMenuId(userId);
        return getMenuList(menuIdList);
    }

    private List<SysMenuEntity> getMenuList(List<Long> menuIdList) {
        List<SysMenuEntity> menus = this.baseMapper.selectList(new LambdaQueryWrapper<SysMenuEntity>()
                .in(Objects.nonNull(menuIdList), SysMenuEntity::getMenuId, menuIdList)
                .eq(SysMenuEntity::getType, 0).or().eq(SysMenuEntity::getType, 1));
        Collections.sort(menus);

        Map<Long, SysMenuEntity> menuMap = new HashMap<>(12);
        for (SysMenuEntity s : menus) {
            menuMap.put(s.getMenuId(), s);
        }
        Iterator<SysMenuEntity> iterator = menus.iterator();
        while (iterator.hasNext()) {
            SysMenuEntity menu = iterator.next();
            SysMenuEntity parent = menuMap.get(menu.getParentId());
            if (Objects.nonNull(parent)) {
                parent.getList().add(menu);
                iterator.remove();
            }
        }
        return menus;
    }

    @Override
    public void delete(Long menuId) {
        this.removeById(menuId);
        sysRoleMenuService.remove(new LambdaQueryWrapper<com.mall.admin.entity.SysRoleMenuEntity>()
                .eq(SysRoleMenuEntity::getMenuId, menuId));
    }
}
