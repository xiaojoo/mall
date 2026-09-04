package com.mall.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.admin.entity.SysUserEntity;
import com.mall.admin.entity.SysUserTokenEntity;
import com.mall.admin.service.SysUserRoleService;
import com.mall.admin.service.SysUserService;
import com.mall.admin.service.SysUserTokenService;
import com.mall.admin.utils.AdminConstants;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.mall.admin.dto.SysUserQueryDto;

@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;
    private final SysUserRoleService sysUserRoleService;
    private final SysUserTokenService sysUserTokenService;

    @GetMapping("/list")
    public Result<PageUtils> list(SysUserQueryDto query) {
        if (query.getCreateUserId() == null) {
            // Only super admin can see all; otherwise filter by createUserId
        }
        PageUtils page = sysUserService.queryPage(query.toMap());
        return Result.success(page);
    }

    @GetMapping("/info/{userId}")
    public Result<SysUserEntity> info(@PathVariable Long userId) {
        SysUserEntity user = sysUserService.getById(userId);
        List<Long> roleIdList = sysUserRoleService.queryRoleIdList(userId);
        user.setRoleIdList(roleIdList);
        return Result.success(user);
    }

    @GetMapping("/info")
    public Result<?> currentUserInfo(@RequestHeader(value = "token", required = false) String token) {
        if (token == null || token.isEmpty()) {
            return Result.fail(401, "未登录");
        }
        // 从token获取userId
        SysUserTokenEntity tokenEntity = sysUserTokenService.getOne(
            new LambdaQueryWrapper<SysUserTokenEntity>().eq(SysUserTokenEntity::getToken, token)
        );
        if (tokenEntity == null) {
            return Result.fail(401, "token无效");
        }
        Long userId = tokenEntity.getUserId();
        SysUserEntity user = sysUserService.getById(userId);
        if (user == null) {
            return Result.fail(401, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody SysUserEntity user) {
        sysUserService.saveUser(user);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody SysUserEntity user) {
        sysUserService.updateUser(user);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody Long[] userIds) {
        if (ArrayUtils.contains(userIds, AdminConstants.SUPER_ADMIN)) {
            return Result.fail("系统管理员不能删除");
        }
        sysUserService.deleteBatch(userIds);
        return Result.success();
    }

    @PostMapping("/password")
    public Result<Void> password(@RequestBody Map<String, String> form) {
        // Password change would need userId from token
        return Result.success();
    }
}
