package com.mall.search.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 属性响应 VO
 */
@Data
public class AttrResponseVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 属性 id
     */
    private Long attrId;

    /**
     * 属性名
     */
    private String attrName;

    /**
     * 属性类型: 0->销售属性，1->基本属性
     */
    private Integer attrType;

    /**
     * 是否 searchable
     */
    private Integer searchable;

    /**
     * 是否显示
     */
    private Integer showStatus;

    /**
     * 所属分组 id
     */
    private Long attrGroupId;

    /**
     * 属性分组名
     */
    private String attrGroupName;

    /**
     * 所属分类树节点
     */
    private Long catelogId;

    /**
     * 所属分类路径
     */
    private Long[] catelogPath;

    /**
     * 检索类型: 0->数字，1->范围，2->枚举，3->滑动范围
     */
    private Integer searchType;

    /**
     * 图标
     */
    private String icon;

    /**
     * 属性值类型: 0->字符串，1->数字，2->日期
     */
    private String valueType;
}
