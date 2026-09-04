package com.mall.auth.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.auth.app.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户
 *
 * @author mall
 */
@Mapper
public interface UserDao extends BaseMapper<UserEntity> {

}
