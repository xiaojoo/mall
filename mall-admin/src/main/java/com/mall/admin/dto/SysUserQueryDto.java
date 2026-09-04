package com.mall.admin.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户名, 创建用户ID查询DTO
 */
@Data
public class SysUserQueryDto {

    private String page;
    private String limit;
    private String sidx;
    private String order;
    private String username;
    private String createUserId;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (page != null) map.put("page", page);
        if (limit != null) map.put("limit", limit);
        if (sidx != null) map.put("sidx", sidx);
        if (order != null) map.put("order", order);
        if (username != null) map.put("username", username);
        if (createUserId != null) map.put("createUserId", createUserId);
        return map;
    }
}