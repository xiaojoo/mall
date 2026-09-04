package com.mall.common.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 分页查询参数 DTO
 *
 * @author mall
 */
@Data
public class PageQueryDTO {

    /**
     * 当前页
     */
    private String page;

    /**
     * 每页条数
     */
    private String limit;

    /**
     * 排序字段
     */
    private String sidx;

    /**
     * 排序方式（asc/desc）
     */
    private String order;

    /**
     * 转换为Map（用于Service层查询）
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (page != null) map.put("page", page);
        if (limit != null) map.put("limit", limit);
        if (sidx != null) map.put("sidx", sidx);
        if (order != null) map.put("order", order);
        return map;
    }
}
