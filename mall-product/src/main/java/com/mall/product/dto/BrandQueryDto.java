package com.mall.product.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 搜索关键字查询DTO
 */
@Data
public class BrandQueryDto {

    private String page;
    private String limit;
    private String sidx;
    private String order;
    private String key;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (page != null) map.put("page", page);
        if (limit != null) map.put("limit", limit);
        if (sidx != null) map.put("sidx", sidx);
        if (order != null) map.put("order", order);
        if (key != null) map.put("key", key);
        return map;
    }
}