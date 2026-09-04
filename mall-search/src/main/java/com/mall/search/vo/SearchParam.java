package com.mall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * cataLog3Id=225&keyword=小米&sort=saleCount_asc&hasStock=0/1&brandId=1&brandId=2
 */
@Data
public class SearchParam {
    /**
     * 页面传递过来的全文匹配关键字
     */
    private String keyword;
    /**
     * 品牌id,可以多选
     */
    private List<Long> brandId;
    /**
     * 三级分类id
     */
    private Long catalog3Id;
    /**
     * 排序条件
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort;
    /**
     * 是否只显示有货， 0无库存，1有库存
     */
    private Integer hasStock = 1;
    /**
     * 价格区间
     */
    private String skuPrice;
    /**
     * 品牌，可以多选
     */
    private List<Long> brandName;
    /**
     * 属性
     */
    private List<String> attrs;
    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 原生的所有查询条件
     */
    private String _queryString;
}
