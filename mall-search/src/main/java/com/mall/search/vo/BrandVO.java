package com.mall.search.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 品牌 VO
 */
@Data
public class BrandVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 品牌名称
     */
    private String name;

    /**
     * 品牌Logo
     */
    private String logo;

    /**
     * 展示状态 [0-不显示，1-显示]
     */
    private Integer showStatus;

    /**
     * 首字母
     */
    private String firstLetter;

    /**
     * 排序
     */
    private Integer sort;
}
