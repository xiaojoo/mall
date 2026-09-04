package com.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 会员
 * @author sunxiaojie
 * @date 2024-08-01 14:14:49
 */
@Data
@TableName("ums_member")
public class MemberEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;
	/**
	 * 会员等级id
	 */
	private Long levelId;
	/**
	 * 用户名
	 */
	private String username;
	/**
	 * 密码
	 */
	private String password;
	/**
	 * 昵称
	 */
	private String nickname;
	/**
	 * 手机号码
	 */
	private String mobile;
	/**
	 * 邮箱
	 */
	private String email;
	/**
	 * 头像
	 */
	private String header;
	/**
	 * 性别
	 */
	private Integer gender;
	/**
	 * 生日
	 */
	private Date birth;
	/**
	 * 所在城市
	 */
	private String city;
	/**
	 * 职业
	 */
	private String job;
	/**
	 * 个性签名
	 */
	private String sign;
	/**
	 * 用户来源
	 */
	private Integer sourceType;
	/**
	 * 积分
	 */
	private Integer integration;
	/**
	 * 成长值
	 */
	private Integer growth;
	/**
	 * 账户余额
	 */
	private BigDecimal balance;
	/**
	 * 启用状态
	 */
	private Integer status;
	/**
	 * 注册时间
	 */
	private Date createTime;

	/**
	 * 社交登录UID
	 */
	private String socialUid;

	/**
	 * 社交登录TOKEN
	 */
	private String accessToken;

	/**
	 * 社交登录过期时间
	 */
	private long expiresIn;
}
