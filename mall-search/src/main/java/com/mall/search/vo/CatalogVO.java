package com.mall.search.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分类 VO
 */
@Data
public class CatalogVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    private Long catId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID
     */
    private Long parentCid;

    /**
     * 子分类列表
     */
    private List<CatalogVO> children;
}
