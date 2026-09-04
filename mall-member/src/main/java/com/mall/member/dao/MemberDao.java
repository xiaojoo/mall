package com.mall.member.dao;

import com.mall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author sunxiaojie
 * @date 2024-08-01 14:14:49
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
