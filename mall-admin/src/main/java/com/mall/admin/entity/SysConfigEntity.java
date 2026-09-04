package com.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统配置信息
 */
@Data
@TableName("sys_config")
public class SysConfigEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private String paramKey;
    private String paramValue;
    private String remark;
    private Date createTime;
}
