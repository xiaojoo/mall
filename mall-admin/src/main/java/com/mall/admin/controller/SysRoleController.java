package com.mall.admin.controller;

import com.mall.admin.entity.SysRoleEntity;
import com.mall.admin.service.SysRoleMenuService;
import com.mall.admin.service.SysRoleService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.mall.admin.dto.SysRoleQueryDto;

@RestController
@RequestMapping("/sys/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;
    private final SysRoleMenuService sysRoleMenuService;

    @GetMapping("/list")
    public Result<PageUtils> list(SysRoleQueryDto query) {
        PageUtils page = sysRoleService.queryPage(query.toMap());
        return Result.success(page);
    }

    @GetMapping("/info/{roleId}")
    public Result<SysRoleEntity> info(@PathVariable Long roleId) {
        SysRoleEntity role = sysRoleService.getById(roleId);
        List<Long> menuIdList = sysRoleMenuService.queryMenuIdList(roleId);
        role.setMenuIdList(menuIdList);
        return Result.success(role);
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody SysRoleEntity role) {
        sysRoleService.saveRole(role);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody SysRoleEntity role) {
        sysRoleService.updateRole(role);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] roleIds) {
        sysRoleService.deleteBatch(roleIds);
        return Result.success();
    }

    @GetMapping("/select")
    public Result<List<SysRoleEntity>> select() {
        List<SysRoleEntity> list = sysRoleService.list();
        return Result.success(list);
    }
}
