package com.mall.admin.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置key查询DTO
 */
@Data
public class SysConfigQueryDto {

    private String page;
    private String limit;
    private String sidx;
    private String order;
    private String paramKey;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (page != null) map.put("page", page);
        if (limit != null) map.put("limit", limit);
        if (sidx != null) map.put("sidx", sidx);
        if (order != null) map.put("order", order);
        if (paramKey != null) map.put("paramKey", paramKey);
        return map;
    }
}