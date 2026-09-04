package com.mall.product.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 属性响应 VO
 * 继承 AttrVo 并添加分组相关信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttrRespVo extends AttrVo {
    /**
     * 属性分组名称
     */
    private String groupName;
    
    /**
     * 分类名称
     */
    private String catelogName;
    
    /**
     * 分类路径
     */
    private Long[] catelogPath;
}
