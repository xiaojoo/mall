package com.mall.ware.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品ID, 仓库ID查询DTO
 */
@Data
public class WareSkuQueryDto {

    private String page;
    private String limit;
    private String sidx;
    private String order;
    private String skuId;
    private String wareId;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (page != null) map.put("page", page);
        if (limit != null) map.put("limit", limit);
        if (sidx != null) map.put("sidx", sidx);
        if (order != null) map.put("order", order);
        if (skuId != null) map.put("skuId", skuId);
        if (wareId != null) map.put("wareId", wareId);
        return map;
    }
}