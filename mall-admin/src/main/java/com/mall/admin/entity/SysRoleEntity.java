package com.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private Long roleId;
    private String roleName;
    private String remark;
    /** 部门ID */
    @TableField(exist = false)
    private Long deptId;
    private Date createTime;
    @TableField(exist = false)
    private List<Long> menuIdList;
}
