package com.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 系统用户
 */
@Data
@TableName("sys_user")
public class SysUserEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long userId;
    private String username;
    private String password;
    private String salt;
    private String email;
    private String mobile;
    /** 状态 0：禁用 1：正常 */
    private Integer status;
    @TableField(exist = false)
    private List<Long> roleIdList;
    private Long createUserId;
    private Date createTime;
}
