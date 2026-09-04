package com.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.admin.entity.SysUserEntity;
import com.mall.admin.exception.BizException;
import com.mall.admin.service.SysRoleService;
import com.mall.admin.service.SysUserRoleService;
import com.mall.admin.service.SysUserService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service("sysUserService")
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<com.mall.admin.dao.SysUserDao, SysUserEntity> implements SysUserService {

    private final SysUserRoleService sysUserRoleService;
    private final SysRoleService sysRoleService;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final long SUPER_ADMIN = 1L;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String username = (String) params.get("username");
        Long createUserId = (Long) params.get("createUserId");

        IPage<SysUserEntity> page = this.page(
                new Query<SysUserEntity>().getPage(params),
                new LambdaQueryWrapper<SysUserEntity>()
                        .like(StringUtils.isNotBlank(username), SysUserEntity::getUsername, username)
                        .eq(createUserId != null, SysUserEntity::getCreateUserId, createUserId)
        );
        return new PageUtils(page);
    }

    @Override
    public List<String> queryAllPerms(Long userId) {
        return baseMapper.queryAllPerms(userId);
    }

    @Override
    public List<Long> queryAllMenuId(Long userId) {
        return baseMapper.queryAllMenuId(userId);
    }

    @Override
    public SysUserEntity queryByUserName(String username) {
        return baseMapper.queryByUserName(username);
    }

    @Override
    @Transactional
    public void saveUser(SysUserEntity user) {
        user.setCreateTime(new Date());
        String salt = generateRandomString(20);
        user.setPassword(PASSWORD_ENCODER.encode(user.getPassword()));
        user.setSalt(salt);
        this.save(user);
        checkRole(user);
        sysUserRoleService.saveOrUpdate(user.getUserId(), user.getRoleIdList());
    }

    @Override
    @Transactional
    public void updateUser(SysUserEntity user) {
        if (StringUtils.isBlank(user.getPassword())) {
            user.setPassword(null);
        } else {
            user.setPassword(PASSWORD_ENCODER.encode(user.getPassword()));
        }
        this.updateById(user);
        checkRole(user);
        sysUserRoleService.saveOrUpdate(user.getUserId(), user.getRoleIdList());
    }

    @Override
    public void deleteBatch(Long[] userIds) {
        this.removeByIds(Arrays.asList(userIds));
    }

    @Override
    public boolean updatePassword(Long userId, String password, String newPassword) {
        SysUserEntity userEntity = new SysUserEntity();
        userEntity.setPassword(newPassword);
        return this.update(userEntity,
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUserId, userId).eq(SysUserEntity::getPassword, password));
    }

    private void checkRole(SysUserEntity user) {
        if (user.getRoleIdList() == null || user.getRoleIdList().isEmpty()) {
            return;
        }
        if (user.getCreateUserId() == SUPER_ADMIN) {
            return;
        }
        List<Long> roleIdList = sysRoleService.queryRoleIdList(user.getCreateUserId());
        if (!roleIdList.containsAll(user.getRoleIdList())) {
            throw new BizException("新增用户所选角色，不是本人创建");
        }
    }

    /**
     * 生成随机字符串（替代已弃用的 RandomStringUtils.randomAlphanumeric）
     */
    private String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes).replaceAll("[^a-zA-Z0-9]", "").substring(0, Math.min(length, bytes.length));
    }
}
