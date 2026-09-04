package com.mall.product.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 关键字, 分类ID, 品牌ID, 最低价, 最高价查询DTO
 */
@Data
public class SkuInfoQueryDto {

    private String page;
    private String limit;
    private String sidx;
    private String order;
    private String key;
    private String catelogId;
    private String brandId;
    private String min;
    private String max;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (page != null) map.put("page", page);
        if (limit != null) map.put("limit", limit);
        if (sidx != null) map.put("sidx", sidx);
        if (order != null) map.put("order", order);
        if (key != null) map.put("key", key);
        if (catelogId != null) map.put("catelogId", catelogId);
        if (brandId != null) map.put("brandId", brandId);
        if (min != null) map.put("min", min);
        if (max != null) map.put("max", max);
        return map;
    }
}