package com.mall.ware.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 关键字, 状态, 仓库ID查询DTO
 */
@Data
public class PurchaseQueryDto {

    private String page;
    private String limit;
    private String sidx;
    private String order;
    private String key;
    private String status;
    private String wareId;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (page != null) map.put("page", page);
        if (limit != null) map.put("limit", limit);
        if (sidx != null) map.put("sidx", sidx);
        if (order != null) map.put("order", order);
        if (key != null) map.put("key", key);
        if (status != null) map.put("status", status);
        if (wareId != null) map.put("wareId", wareId);
        return map;
    }
}