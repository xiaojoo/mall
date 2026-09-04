package com.mall.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 秒杀活动商品关联
 * 
 * @author sunxiaojie
 * @date 2024-08-01 13:52:57
 */
@Data
@TableName("sms_seckill_sku_relation")
public class SeckillSkuRelationEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;
	/**
	 * 活动id
	 */
	private Long promotionId;
	/**
	 * 活动场次id
	 */
	private Long promotionSessionId;
	/**
	 * 商品id
	 */
	private Long skuId;
	/**
	 * 秒杀价格
	 */
	private BigDecimal seckillPrice;
	/**
	 * 秒杀总量
	 */
	private BigDecimal seckillCount;
	/**
	 * 每人限购数量
	 */
	private BigDecimal seckillLimit;
	/**
	 * 排序
	 */
	private Integer seckillSort;

	/**
	 * 上架状态（1=上架 0=下架；持久化，Redis 重建后以此为准，默认 1）
	 */
	private Integer shelfStatus;

	/**
	 * 下架时间（审计）
	 */
	private Date offShelfTime;

	/**
	 * 上架时间（审计）
	 */
	private Date onShelfTime;

	/**
	 * 实时剩余库存（查询时从 Redis 填充，不落库）
	 */
	@TableField(exist = false)
	private Integer stock;

}
