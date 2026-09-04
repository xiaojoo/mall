package com.mall.auth.perm.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 操作日志
 */
@Data
@TableName("sys_log")
public class SysLogEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long userId;
    private String username;
    private String operation;
    private String method;
    private String params;
    private String ip;
    /** 状态：0-失败 1-成功 */
    private Integer status;
    private String errorMsg;
    private Date createTime;
}
