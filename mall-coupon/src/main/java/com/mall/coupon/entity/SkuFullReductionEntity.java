package com.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 商品满减信息
 * 
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
@Data
@TableName("sms_sku_full_reduction")
public class SkuFullReductionEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;
	/**
	 * spu_id
	 */
	private Long skuId;
	/**
	 * 满多少
	 */
	private BigDecimal fullPrice;
	/**
	 * 减多少
	 */
	private BigDecimal reducePrice;
	/**
	 * 是否参与其他优惠
	 */
	private Integer addOther;

}
