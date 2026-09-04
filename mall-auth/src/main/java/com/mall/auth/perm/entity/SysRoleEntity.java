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
 * 系统角色
 */
@Data
@TableName("sys_role")
public class SysRoleEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;
    private String roleName;
    private String roleCode;
    private String remark;
    /** 状态：0-禁用 1-正常 */
    private Integer status;
    private Date createTime;
    private Date updateTime;

    /** 菜单ID列表（非数据库字段） */
    @TableField(exist = false)
    private List<String> menuIdList;
}
