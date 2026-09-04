package com.mall.admin.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户名查询DTO
 */
@Data
public class SysLogQueryDto {

    private String page;
    private String limit;
    private String sidx;
    private String order;
    private String username;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (page != null) map.put("page", page);
        if (limit != null) map.put("limit", limit);
        if (sidx != null) map.put("sidx", sidx);
        if (order != null) map.put("order", order);
        if (username != null) map.put("username", username);
        return map;
    }
}