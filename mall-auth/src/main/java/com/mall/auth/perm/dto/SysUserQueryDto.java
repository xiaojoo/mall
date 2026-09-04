package com.mall.auth.perm.dto;

import lombok.Data;

/**
 * 用户查询参数
 */
@Data
public class SysUserQueryDto {
    private String username;
    private Integer status;
    private Long deptId;
    private Integer page = 1;
    private Integer limit = 10;
}
