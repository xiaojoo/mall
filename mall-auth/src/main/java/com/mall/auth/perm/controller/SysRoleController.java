package com.mall.auth.perm.controller;

import com.mall.auth.perm.annotation.RequirePermission;
import com.mall.auth.perm.entity.SysRoleEntity;
import com.mall.auth.perm.service.SysRoleService;
import com.mall.common.exception.RRException;
import com.mall.common.utils.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 系统角色管理
 * 前端期望：
 *   list: { code: 0, page: { list, totalCount } }
 *   info: { code: 0, role: { ... } }
 *   select: { code: 0, list: [...] }
 */
@RestController
@RequestMapping("/sys/role")
@RequiredArgsConstructor
@Tag(name = "系统角色管理", description = "系统角色CRUD接口")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping("/list")
    @Operation(summary = "角色分页列表")
    @RequirePermission("sys:role:list")
    public Map<String, Object> list(@RequestParam Map<String, Object> params) {
        PageUtils page = sysRoleService.queryPage(params);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("page", page);
        return result;
    }

    @GetMapping("/info/{roleId}")
    @Operation(summary = "角色详情")
    @RequirePermission("sys:role:list")
    public Map<String, Object> info(@PathVariable Long roleId) {
        SysRoleEntity role = sysRoleService.getById(roleId);
        if (role == null) {
            throw new RRException("角色不存在或已被删除");
        }
        role.setMenuIdList(sysRoleService.queryMenuIdList(roleId).stream().map(String::valueOf).collect(java.util.stream.Collectors.toList()));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("role", role);
        return result;
    }

    @PostMapping("/save")
    @Operation(summary = "新增角色")
    @RequirePermission("sys:role:save")
    public Map<String, Object> save(@RequestBody SysRoleEntity role) {
        sysRoleService.createRole(role);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @PostMapping("/update")
    @Operation(summary = "修改角色")
    @RequirePermission("sys:role:update")
    public Map<String, Object> update(@RequestBody SysRoleEntity role) {
        sysRoleService.updateRole(role);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除角色")
    @RequirePermission("sys:role:delete")
    public Map<String, Object> delete(@RequestBody Long[] roleIds) {
        sysRoleService.deleteBatch(roleIds);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @GetMapping("/select")
    @RequirePermission("sys:role:list")
    @Operation(summary = "角色下拉列表")
    public Map<String, Object> select() {
        List<SysRoleEntity> list = sysRoleService.list();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("list", list);
        return result;
    }
}
