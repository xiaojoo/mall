package com.mall.auth.perm.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 系统菜单/权限
 */
@Data
@TableName("sys_menu")
public class SysMenuEntity implements Serializable, Comparable<SysMenuEntity> {
    private static final long serialVersionUID = 1L;

    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long menuId;
    private Long parentId;
    private String name;
    private String url;
    /** 授权标识，如 sys:user:list */
    private String perms;
    /** 类型：0-目录 1-菜单 2-按钮 */
    private Integer type;
    private String icon;
    private Integer orderNum;
    /** 状态：0-禁用 1-正常 */
    private Integer status;
    private Date createTime;
    private Date updateTime;

    /** 子菜单列表（非数据库字段） */
    @TableField(exist = false)
    private List<SysMenuEntity> list = new ArrayList<>();

    @Override
    public int compareTo(SysMenuEntity o) {
        int thisOrder = this.orderNum != null ? this.orderNum : 0;
        int otherOrder = o.orderNum != null ? o.orderNum : 0;
        return Integer.compare(thisOrder, otherOrder);
    }
}
