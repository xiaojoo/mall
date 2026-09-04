package com.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户Token
 */
@Data
@TableName("sys_user_token")
public class SysUserTokenEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long userId;
    private String token;
    private Date updateTime;
    private Date expireTime;
}
