package com.mall.admin.controller;

import com.mall.admin.entity.SysMenuEntity;
import com.mall.admin.service.SysMenuService;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/list")
    public Result<List<SysMenuEntity>> list() {
        List<SysMenuEntity> menuList = sysMenuService.queryNotButtonList();
        return Result.success(menuList);
    }

    @GetMapping("/nav")
    public Result<List<SysMenuEntity>> nav(@RequestParam Long userId) {
        List<SysMenuEntity> menuList = sysMenuService.getUserMenuList(userId);
        return Result.success(menuList);
    }

    @GetMapping("/info/{menuId}")
    public Result<SysMenuEntity> info(@PathVariable Long menuId) {
        SysMenuEntity menu = sysMenuService.getById(menuId);
        return Result.success(menu);
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody SysMenuEntity menu) {
        if (menu.getMenuId() == null && menu.getId() != null) {
            menu.setMenuId(menu.getId());
        }
        sysMenuService.save(menu);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody SysMenuEntity menu) {
        // 兼容前端发送 id 字段
        if (menu.getMenuId() == null && menu.getId() != null) {
            menu.setMenuId(menu.getId());
        }
        sysMenuService.updateById(menu);
        return Result.success();
    }

    @PostMapping("/delete/{menuId}")
    public Result<Void> delete(@PathVariable Long menuId) {
        sysMenuService.delete(menuId);
        return Result.success();
    }

    @GetMapping("/select")
    public Result<Map<String, List<SysMenuEntity>>> select() {
        List<SysMenuEntity> menuList = sysMenuService.queryNotButtonList();
        Map<String, List<SysMenuEntity>> result = Map.of("menuList", menuList);
        return Result.success(result);
    }
}
