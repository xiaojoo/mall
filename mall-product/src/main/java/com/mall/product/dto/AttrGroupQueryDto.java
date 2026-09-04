package com.mall.product.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 搜索关键字查询DTO
 */
@Data
public class AttrGroupQueryDto {

    private String page;
    private String limit;
    private String sidx;
    private String order;
    private String key;
    private String catelogId;

    /**
     * 解析 catelogId，非法/空值返回 null
     */
    public Long parseCatelogId() {
        if (catelogId == null || catelogId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(catelogId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

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