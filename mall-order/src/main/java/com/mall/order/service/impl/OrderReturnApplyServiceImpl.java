package com.mall.order.service.impl;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.util.StringUtils;
import com.mall.common.exception.RRException;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.order.config.AlipayTemplate;
import com.mall.order.dao.OrderReturnApplyDao;
import com.mall.order.entity.OrderReturnApplyEntity;
import com.mall.order.entity.RefundInfoEntity;
import com.mall.order.service.OrderReturnApplyService;
import com.mall.order.service.RefundInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("orderReturnApplyService")
@RequiredArgsConstructor
public class OrderReturnApplyServiceImpl extends ServiceImpl<OrderReturnApplyDao, OrderReturnApplyEntity> implements OrderReturnApplyService {

    private final RefundInfoService refundInfoService;

    private final AlipayTemplate alipayTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String status = (String) params.get("status");
        IPage<OrderReturnApplyEntity> page = this.page(
                new Query<OrderReturnApplyEntity>().getPage(params),
                new LambdaQueryWrapper<OrderReturnApplyEntity>()
                        .eq(StringUtils.hasText(status), OrderReturnApplyEntity::getStatus, status)
                        .orderByDesc(OrderReturnApplyEntity::getCreateTime)
        );

        return new PageUtils(page);
    }

    /**
     * 审核通过即退款：调支付宝退款，结果写入 oms_refund_info，
     * 退款成功后将申请单状态置为已完成(2)，失败则保持原状态并抛出异常。
     * out_request_no 固定用订单号，与订单已关闭自动退款（refundIfClosed）一致，保证幂等。
     */
    @Override
    public void approve(Long id) {
        OrderReturnApplyEntity apply = this.getById(id);
        if (apply == null) {
            throw new RRException("退款申请不存在");
        }
        Integer status = apply.getStatus();
        // 仅待处理(0)/退货中(1) 可审核通过，防止重复退款
        if (status == null || (status != 0 && status != 1)) {
            throw new RRException("当前售后状态不可审核通过");
        }
        String orderSn = apply.getOrderSn();
        BigDecimal amount = apply.getReturnAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RRException("退款金额无效");
        }
        // 落库退款记录（先记处理中，成功后补状态）
        RefundInfoEntity refundInfo = new RefundInfoEntity();
        refundInfo.setOrderReturnId(apply.getId());
        refundInfo.setRefund(amount);
        refundInfo.setRefundSn(orderSn);
        refundInfo.setRefundStatus(0);
        refundInfo.setRefundChannel(1);
        refundInfo.setRefundContent("售后审核通过退款，订单号：" + orderSn + "，金额：" + amount.toPlainString());
        String code;
        try {
            code = alipayTemplate.refund(orderSn, amount);
        } catch (Exception e) {
            refundInfo.setRefundStatus(0);
            refundInfo.setRefundContent(refundInfo.getRefundContent() + "；异常：" + e.getMessage());
            refundInfoService.save(refundInfo);
            log.error("售后审核通过，退款异常: id={}, orderSn={}", id, orderSn, e);
            throw new RRException("退款调用失败：" + e.getMessage());
        }
        if (!"10000".equals(code)) {
            refundInfo.setRefundStatus(0);
            refundInfo.setRefundContent(refundInfo.getRefundContent() + "；支付宝返回码：" + code);
            refundInfoService.save(refundInfo);
            log.warn("售后审核通过，退款失败: id={}, orderSn={}, alipayCode={}", id, orderSn, code);
            throw new RRException("退款失败，支付宝返回码：" + code);
        }
        refundInfo.setRefundStatus(1);
        refundInfoService.save(refundInfo);
        log.info("售后审核通过，退款成功: id={}, orderSn={}, amount={}", id, orderSn, amount);
        // 退款成功 → 申请单状态置为已完成(2)
        OrderReturnApplyEntity update = new OrderReturnApplyEntity();
        update.setId(apply.getId());
        update.setStatus(2);
        update.setHandleTime(new Date());
        this.updateById(update);
    }
}
