package com.mall.auth.perm.controller;

import com.mall.auth.perm.annotation.RequirePermission;
import com.mall.auth.perm.entity.SysUserEntity;
import com.mall.auth.perm.jwt.JwtTokenService;
import com.mall.auth.perm.service.SysUserService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 系统用户管理
 * 前端期望返回格式：
 *   list: { code: 0, page: { list, totalCount, pageSize, currPage, totalPage } }
 *   info: { code: 0, user: { ... } }
 *   save/update/delete: { code: 0 }
 */
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
@Tag(name = "系统用户管理", description = "系统用户CRUD接口")
public class SysUserController {

    private final SysUserService sysUserService;

    private final JwtTokenService jwtTokenService;

    @GetMapping("/list")
    @Operation(summary = "用户分页列表")
    @RequirePermission("sys:user:list")
    public Map<String, Object> list(@RequestParam Map<String, Object> params) {
        PageUtils page = sysUserService.queryPage(params);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("page", page);
        return result;
    }

    @GetMapping("/info/{userId}")
    @Operation(summary = "用户详情")
    @RequirePermission("sys:user:list")
    public Map<String, Object> info(@PathVariable Long userId) {
        SysUserEntity user = sysUserService.getById(userId);
        if (user != null) {
            user.setRoleIdList(sysUserService.queryRoleIdList(userId).stream().map(String::valueOf).collect(java.util.stream.Collectors.toList()));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("user", user);
        return result;
    }

    @PostMapping("/save")
    @Operation(summary = "新增用户")
    @RequirePermission("sys:user:save")
    public Map<String, Object> save(@RequestBody SysUserEntity user) {
        sysUserService.createUser(user);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @PostMapping("/update")
    @Operation(summary = "修改用户")
    @RequirePermission("sys:user:update")
    public Map<String, Object> update(@RequestBody SysUserEntity user) {
        sysUserService.updateUser(user);
        // 禁用账号即时生效：吊销该用户全部登录态
        if (user.getStatus() != null && user.getStatus() == 0) {
            jwtTokenService.kickUser(user.getUserId());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    /**
     * 踢人：吊销指定用户全部登录态（access 立即失效 + refresh 作废），需重新登录
     */
    @PostMapping("/kick")
    @Operation(summary = "踢人（强制下线）")
    @RequirePermission("sys:user:update")
    public Map<String, Object> kick(@RequestBody Map<String, Object> params) {
        Long userId = Long.parseLong(params.get("userId").toString());
        jwtTokenService.kickUser(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除用户")
    @RequirePermission("sys:user:delete")
    public Map<String, Object> delete(@RequestBody Long[] userIds) {
        sysUserService.deleteBatch(userIds);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @PostMapping("/resetPassword")
    @Operation(summary = "重置密码")
    @RequirePermission("sys:user:resetpwd")
    public Map<String, Object> resetPassword(@RequestBody Map<String, Object> params) {
        Long userId = Long.parseLong(params.get("userId").toString());
        String newPassword = params.get("newPassword").toString();
        sysUserService.resetPassword(userId, newPassword);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        return result;
    }

    @PostMapping("/updatePassword")
    @Operation(summary = "修改本人密码")
    public Map<String, Object> updatePassword(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        // 以登录态 userId 为准（忽略 body 传入的 userId，防止越权改他人密码）
        Object userIdObj = request.getAttribute("userId");
        Map<String, Object> result = new HashMap<>();
        if (userIdObj == null) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        Long userId = Long.parseLong(userIdObj.toString());
        String oldPassword = params.get("oldPassword").toString();
        String newPassword = params.get("newPassword").toString();
        sysUserService.updatePassword(userId, oldPassword, newPassword);
        result.put("code", 0);
        return result;
    }

    /**
     * 当前登录用户资料（头像弹窗展示/编辑用，无需管理权限）
     */
    @GetMapping("/profile")
    @Operation(summary = "当前用户资料")
    public Map<String, Object> profile(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        Map<String, Object> result = new HashMap<>();
        if (userIdObj == null) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        SysUserEntity user = sysUserService.getById(Long.parseLong(userIdObj.toString()));
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "用户不存在");
            return result;
        }
        user.setPassword(null);
        user.setSalt(null);
        result.put("code", 0);
        result.put("user", user);
        return result;
    }

    /**
     * 修改当前用户资料（仅本人：email/mobile/realName/avatar）
     */
    @PostMapping("/profile")
    @Operation(summary = "修改当前用户资料")
    public Map<String, Object> updateProfile(@RequestBody SysUserEntity form, HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        Map<String, Object> result = new HashMap<>();
        if (userIdObj == null) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }
        Long userId = Long.parseLong(userIdObj.toString());
        // 只允许更新资料字段（MP 忽略 null，其余字段不会被动到）
        SysUserEntity update = new SysUserEntity();
        update.setUserId(userId);
        update.setEmail(form.getEmail());
        update.setMobile(form.getMobile());
        update.setRealName(form.getRealName());
        update.setAvatar(form.getAvatar());
        update.setUpdateTime(new Date());
        sysUserService.updateById(update);
        result.put("code", 0);
        return result;
    }

    @GetMapping("/perms")
    @Operation(summary = "当前用户权限标识列表")
    public Map<String, Object> perms(HttpServletRequest request) {
        // userId 由 PermissionInterceptor 从 JWT 解析后写入 request attribute
        Object userIdObj = request.getAttribute("userId");
        Map<String, Object> result = new HashMap<>();
        if (userIdObj == null) {
            result.put("code", 401);
            return result;
        }
        Long userId = Long.parseLong(userIdObj.toString());
        List<String> perms = sysUserService.queryPermsList(userId);
        result.put("code", 0);
        result.put("perms", perms);
        return result;
    }
}
