package com.mall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.to.mq.SeckillOrderTo;
import com.mall.common.utils.PageUtils;
import com.mall.order.entity.OrderEntity;
import com.mall.order.entity.OrderItemEntity;
import com.mall.order.entity.OrderReturnApplyEntity;
import com.mall.order.vo.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author sunxiaojie
 * @date 2024-08-01 14:32:06
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 校验会员是否购买过某商品且已支付成功（订单状态：1 待发货 / 2 已发货 / 3 已完成）
     *
     * @param memberId 会员 id
     * @param skuId    sku id（可为空）
     * @param spuId    spu id（可为空，skuId 与 spuId 至少一个）
     * @return 已购买且支付成功返回 true
     */
    boolean hasPaidOrder(Long memberId, Long skuId, Long spuId);

    /**
     * 订单确认页返回需要的数据
     */
    OrderConfirmVo confirmOrder(OrderConfirmVo confirmVo) throws ExecutionException, InterruptedException;

    /**
     * 创建订单
     */
    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    /**
     * 按照订单号获取订单信息
     */
    OrderEntity getOrderByOrderSn(String orderSn);

    /**
     * 关闭订单
     */
    void closeOrder(OrderEntity entity);

    /**
     * 取消订单（仅待付款订单可取消；秒杀订单同步回补 Redis 秒杀库存）
     *
     * @param orderSn 订单号
     */
    void cancelOrder(String orderSn);

    /**
     * 申请退款（仅已付款订单；同一订单仅一笔进行中的申请）
     * 校验：登录态（JWT）+ 订单归属 + 状态 1/2/3 + 重复申请，通过后写入 oms_order_return_apply（status=0 待处理）
     */
    void refundApply(RefundApplyVo vo, HttpServletRequest request);

    /**
     * 确认收货：已付款（1 待发货 / 2 已发货）订单置为已完成（3），记录收货时间
     */
    void receiveOrder(String orderSn, HttpServletRequest request);

    /**
     * 查询订单最近一笔售后申请（无则返回 null）
     */
    OrderReturnApplyEntity getLatestRefundApply(String orderSn);

    /**
     * 售后中订单总数（该会员订单中存在进行中售后申请 0/1/2 的单数）
     */
    long countAfterSaleOrders(Long memberId);

    /**
     * 进行中的售后订单号列表（0 待处理 / 1 退货中）——状态 tab 筛选时排除，只在「售后中」tab 展示
     */
    List<String> listActiveAfterSaleSns(Long memberId);

    /**
     * 秒杀建单失败回滚：撤销占位 + 回补 Redis 秒杀库存
     *
     * @param orderTo 秒杀订单消息体
     */
    void rollbackSeckillStock(SeckillOrderTo orderTo);

    /**
     * 确认支付结果：已支付直接返回；否则主动查支付宝交易状态，成功则更新订单
     *
     * @param orderSn 订单号
     * @return 当前订单状态码
     */
    int confirmPayStatus(String orderSn);

    /**
     * 支付宝异步通知处理订单数据
     */
    PayVo getOrderPay(String orderSn);

    /**
     * 查询当前用户所有订单数据
     */
    PageUtils queryPageWithItem(Map<String, Object> params);

    /**
     * 支付宝异步通知处理订单数据
     */
    String handlePayResult(PayAsyncVo asyncVo);

    /**
     * 创建秒杀单
     */
    void createSeckillOrder(SeckillOrderTo orderTo);

    /**
     * 补全订单项的品牌 id/名称（店铺跳转用；获取失败不阻塞）
     */
    void fillItemBrandIds(List<OrderItemEntity> items);
}

