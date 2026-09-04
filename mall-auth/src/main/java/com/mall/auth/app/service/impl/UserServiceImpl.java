package com.mall.auth.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.exception.RRException;
import com.mall.common.validator.Assert;
import com.mall.auth.app.dao.UserDao;
import com.mall.auth.app.entity.UserEntity;
import com.mall.auth.app.form.LoginForm;
import com.mall.auth.app.service.UserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserDao, UserEntity> implements UserService {
    @Override
    public UserEntity queryByMobile(String mobile) {
        return baseMapper.selectOne(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getMobile, mobile));
    }

    @Override
    public long login(LoginForm form) {
        UserEntity user = queryByMobile(form.getMobile());
        Assert.isNull(user, "手机号或密码错误");
        //密码错误
        if(!user.getPassword().equals(DigestUtils.sha256Hex(form.getPassword()))){
            throw new RRException("手机号或密码错误");
        }
        return user.getUserId();
    }

    @Override
    public void register(UserEntity user) {
        // 检查手机号是否已注册
        UserEntity existUser = queryByMobile(user.getMobile());
        if (existUser != null) {
            throw new RRException("手机号已注册");
        }
        // 加密密码
        user.setPassword(DigestUtils.sha256Hex(user.getPassword()));
        user.setCreateTime(new Date());
        this.save(user);
    }

    @Override
    public void updatePassword(Long userId, String password) {
        UserEntity user = this.getById(userId);
        if (user == null) {
            throw new RRException("用户不存在");
        }
        user.setPassword(DigestUtils.sha256Hex(password));
        this.updateById(user);
    }
}
