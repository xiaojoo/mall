package com.mall.auth.perm.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.auth.perm.entity.SysMenuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysMenuDao extends BaseMapper<SysMenuEntity> {

    /**
     * 根据父ID查询子菜单列表
     */
    List<SysMenuEntity> queryListParentId(@Param("parentId") Long parentId);

    /**
     * 查询非按钮类型菜单
     */
    List<SysMenuEntity> queryNotButtonList();

    /**
     * 查询用户的所有菜单ID
     */
    List<Long> queryMenuIdList(@Param("userId") Long userId);

    /**
     * 查询用户的所有权限标识（含按钮 type=2，去重、去空）
     */
    List<String> queryAllPerms(@Param("userId") Long userId);
}
