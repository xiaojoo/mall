package com.mall.product.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 关键字, 状态, 品牌ID, 分类ID查询DTO
 */
@Data
public class SpuInfoQueryDto {

    private String page;
    private String limit;
    private String sidx;
    private String order;
    private String key;
    private String status;
    private String brandId;
    private String catelogId;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (page != null) map.put("page", page);
        if (limit != null) map.put("limit", limit);
        if (sidx != null) map.put("sidx", sidx);
        if (order != null) map.put("order", order);
        if (key != null) map.put("key", key);
        if (status != null) map.put("status", status);
        if (brandId != null) map.put("brandId", brandId);
        if (catelogId != null) map.put("catelogId", catelogId);
        return map;
    }
}