package com.mall.auth.perm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
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
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String username;
    private String password;
    private String salt;
    private String email;
    private String mobile;
    private String realName;
    private String avatar;
    private Long deptId;
    /** 状态：0-禁用 1-正常 */
    private Integer status;
    private Date createTime;
    private Date updateTime;

    /** 角色ID列表（非数据库字段） */
    @TableField(exist = false)
    private List<String> roleIdList;

    /** 角色名称列表（非数据库字段） */
    @TableField(exist = false)
    private List<String> roleNameList;
}
