package com.mall.auth.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.auth.app.entity.UserEntity;
import com.mall.auth.app.form.LoginForm;

/**
 * 用户服务
 *
 * @author mall
 */
public interface UserService extends IService<UserEntity> {
    UserEntity queryByMobile(String mobile);

    /**
     * 用户登录
     * @param form 登录表单
     * @return 返回用户 ID
     */
    long login(LoginForm form);

    void register(UserEntity user);

    void updatePassword(Long userId, String password);
}
