package com.mall.member.vo;

import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 会员收藏商品展示 VO（收藏页渲染：名称/主图/价格/分类/跳转 SKU）
 */
@Data
public class CollectSpuVo {

    /** 收藏记录 id（雪花 id 转字符串，防 JS 精度丢失） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** spu_id */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long spuId;

    /** 商品名称 */
    private String spuName;

    /** 商品主图 */
    private String spuImg;

    /** 首个 SKU id（详情页跳转用） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long skuId;

    /** 价格 */
    private BigDecimal price;

    /** 所属分类 id */
    private Long catalogId;

    /** 分类名称 */
    private String categoryName;

    /** 收藏时间 */
    private Date createTime;

    /** 收藏时的商品参数（JSON） */
    private String skuParams;
}
