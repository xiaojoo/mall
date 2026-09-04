package com.mall.auth.perm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.auth.perm.dao.SysMenuDao;
import com.mall.auth.perm.entity.SysMenuEntity;
import com.mall.auth.perm.entity.SysRoleMenuEntity;
import com.mall.auth.perm.service.SysMenuService;
import com.mall.auth.perm.service.SysRoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("sysMenuService")
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuDao, SysMenuEntity> implements SysMenuService {

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
    public List<String> queryAllPerms(Long userId) {
        return baseMapper.queryAllPerms(userId);
    }

    @Override
    public List<SysMenuEntity> getUserMenuList(Long userId) {
        if (userId == SUPER_ADMIN) {
            return getMenuList(null);
        }
        List<Long> menuIdList = baseMapper.queryMenuIdList(userId);
        return getMenuList(menuIdList);
    }

    private List<SysMenuEntity> getMenuList(List<Long> menuIdList) {
        // 空菜单集合：非超管账号未绑定任何菜单，直接返回空，避免 IN () 语法错误
        if (menuIdList != null && menuIdList.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysMenuEntity> menus = this.baseMapper.selectList(
                new LambdaQueryWrapper<SysMenuEntity>()
                        .in(Objects.nonNull(menuIdList), SysMenuEntity::getMenuId, menuIdList)
                        .and(w -> w.eq(SysMenuEntity::getType, 0).or().eq(SysMenuEntity::getType, 1))
                        // 禁用菜单（status=0）不出现在导航；老数据 status 可能为 NULL，视为正常
                        .and(w -> w.eq(SysMenuEntity::getStatus, 1).or().isNull(SysMenuEntity::getStatus))
        );
        Collections.sort(menus);

        Map<Long, SysMenuEntity> menuMap = new HashMap<>(16);
        for (SysMenuEntity s : menus) {
            menuMap.put(s.getMenuId(), s);
        }
        Iterator<SysMenuEntity> iterator = menus.iterator();
        while (iterator.hasNext()) {
            SysMenuEntity menu = iterator.next();
            SysMenuEntity parent = menuMap.get(menu.getParentId());
            // 防御：parentId 指向自己时跳过，避免自引用导致序列化死循环
            if (Objects.nonNull(parent) && !parent.getMenuId().equals(menu.getMenuId())) {
                parent.getList().add(menu);
                iterator.remove();
            }
        }
        return menus;
    }

    @Override
    public void delete(Long menuId) {
        this.removeById(menuId);
        sysRoleMenuService.remove(new LambdaQueryWrapper<SysRoleMenuEntity>()
                .eq(SysRoleMenuEntity::getMenuId, menuId));
    }
}
