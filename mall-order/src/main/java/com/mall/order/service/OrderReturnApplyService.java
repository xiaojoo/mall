package com.mall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.order.entity.OrderReturnApplyEntity;

import java.util.Map;

/**
 * 订单退货申请
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:32:06
 */
public interface OrderReturnApplyService extends IService<OrderReturnApplyEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 审核通过即退款：调支付宝退款，成功后将申请单状态置为已完成(2)
     */
    void approve(Long id);
}

