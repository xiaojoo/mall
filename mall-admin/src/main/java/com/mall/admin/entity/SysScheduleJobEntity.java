package com.mall.admin.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("schedule_job")
public class SysScheduleJobEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long jobId;
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String cronExpression;
    private Integer misfirePolicy;
    private Integer concurrent;
    private Integer status;
    private String remark;
    private Date createTime;
}
