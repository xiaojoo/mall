package com.mall.auth.perm.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.auth.perm.annotation.RequirePermission;
import com.mall.auth.perm.entity.SysMenuEntity;
import com.mall.auth.perm.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 系统菜单管理
 * 前端期望：
 *   list: 直接返回数组 [{ ... }, { ... }]
 *   select: { menuList: [...] }
 *   info: { code: 0, menu: { ... } }
 *   nav: 直接返回数组
 */
@RestController
@RequestMapping("/sys/menu")
@RequiredArgsConstructor
@Tag(name = "系统菜单管理", description = "系统菜单CRUD接口")
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/list")
    @Operation(summary = "菜单列表（树形，含按钮）")
    @RequirePermission("sys:menu:list")
    public List<SysMenuEntity> list() {
        // 含 type=2 按钮节点，角色授权树才能勾选按钮权限
        return sysMenuService.list(new LambdaQueryWrapper<SysMenuEntity>()
                .orderByAsc(SysMenuEntity::getOrderNum));
    }

    @GetMapping("/nav")
    @Operation(summary = "当前用户的导航菜单")
    public List<SysMenuEntity> nav(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            return Collections.emptyList();
        }
        Long userId = Long.parseLong(userIdObj.toString());
        return sysMenuService.getUserMenuList(userId);
    }

    @GetMapping("/info/{menuId}")
    @Operation(summary = "菜单详情")
    @RequirePermission("sys:menu:list")
    public Map<String, Object> info(@PathVariable Long menuId) {
        SysMenuEntity menu = sysMenuService.getById(menuId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("menu", menu);
        return result;
    }

    @PostMapping("/save")
    @Operation(summary = "新增菜单")
    @RequirePermission("sys:menu:save")
    public Map<String, Object> save(@RequestBody SysMenuEntity menu) {
        sysMenuService.save(menu);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @PostMapping("/update")
    @Operation(summary = "修改菜单")
    @RequirePermission("sys:menu:update")
    public Map<String, Object> update(@RequestBody SysMenuEntity menu) {
        sysMenuService.updateById(menu);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @PostMapping("/delete/{menuId}")
    @Operation(summary = "删除菜单")
    @RequirePermission("sys:menu:delete")
    public Map<String, Object> delete(@PathVariable Long menuId) {
        sysMenuService.delete(menuId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @GetMapping("/select")
    @RequirePermission("sys:menu:list")
    @Operation(summary = "菜单下拉树（用于角色分配菜单）")
    public Map<String, Object> select() {
        List<SysMenuEntity> menuList = sysMenuService.queryNotButtonList();
        Map<String, Object> result = new HashMap<>();
        result.put("menuList", menuList);
        return result;
    }
}
