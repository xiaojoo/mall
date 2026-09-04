package com.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 系统菜单
 */
@Data
@TableName("sys_menu")
public class SysMenuEntity implements Serializable, Comparable<SysMenuEntity> {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long menuId;
    
    @TableField(exist = false)
    private Long id;
    private Long parentId;
    private String name;
    private String url;
    private String perms;
    private Integer type;
    private String icon;
    private Integer orderNum;
    @TableField(exist = false)
    private List<SysMenuEntity> list = new ArrayList<>();

    @Override
    public int compareTo(SysMenuEntity o) {
        int thisOrder = this.orderNum != null ? this.orderNum : 0;
        int otherOrder = o.orderNum != null ? o.orderNum : 0;
        return Integer.compare(thisOrder, otherOrder);
    }
}
