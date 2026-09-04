package com.mall.auth.perm.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.auth.perm.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserDao extends BaseMapper<SysUserEntity> {

    /**
     * 查询用户的所有角色ID
     */
    List<Long> queryRoleIdList(@Param("userId") Long userId);
}
