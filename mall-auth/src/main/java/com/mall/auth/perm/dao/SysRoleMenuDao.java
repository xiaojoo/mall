package com.mall.auth.perm.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.auth.perm.entity.SysRoleMenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMenuDao extends BaseMapper<SysRoleMenuEntity> {

    /**
     * 查询角色的所有菜单ID
     */
    List<Long> queryMenuIdList(@Param("roleId") Long roleId);
}
