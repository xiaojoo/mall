package com.mall.order.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import java.util.UUID;
import com.mall.common.exception.NoStockException;
import com.mall.common.exception.RRException;
import com.mall.common.jwt.MemberJwtUtils;
import com.mall.common.to.CouponUseCheckTo;
import com.mall.common.to.mq.OrderTo;
import com.mall.common.to.mq.SeckillOrderTo;
import com.mall.common.utils.Result;
import com.mall.common.utils.ResultUtil;
import com.mall.common.vo.MemberResponseVo;
import com.mall.order.constant.PayConstant;
import com.mall.order.entity.OrderItemEntity;
import com.mall.order.entity.OrderReturnApplyEntity;
import com.mall.order.entity.PaymentInfoEntity;
import com.mall.order.entity.RefundInfoEntity;
import com.mall.order.enume.OrderStatusEnum;
import com.mall.order.feign.CartFeignService;
import com.mall.order.feign.CouponFeignService;
import com.mall.order.feign.MemberFeignService;
import com.mall.order.feign.ProductFeignService;
import com.mall.order.feign.WmsFeignService;
import com.mall.order.interceptor.LoginUserInterceptor;
import com.mall.order.service.OrderItemService;
import com.mall.order.service.OrderReturnApplyService;
import com.mall.order.service.PaymentInfoService;
import com.mall.order.service.RefundInfoService;
import com.mall.order.to.OrderCreateTo;
import com.mall.order.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.order.config.AlipayTemplate;
import com.mall.order.dao.OrderDao;
import com.mall.order.entity.OrderEntity;
import com.mall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static com.mall.order.constant.OrderConstant.USER_ORDER_TOKEN_PREFIX;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("orderService")
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();


    final MemberFeignService memberFeignService;


    final CartFeignService cartFeignService;


    final WmsFeignService wmsFeignService;


    final CouponFeignService couponFeignService;


    final StringRedisTemplate redisTemplate;


    private final RabbitTemplate rabbitTemplate;


    final ProductFeignService productFeignService;


    private final OrderItemService orderItemService;


    private final PaymentInfoService paymentInfoService;


    private final RefundInfoService refundInfoService;


    private final OrderReturnApplyService orderReturnApplyService;


    private final MemberJwtUtils memberJwtUtils;


    private final AlipayTemplate alipayTemplate;


    private final ThreadPoolExecutor threadPoolExecutor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        // 管理端订单列表：key 按订单号/会员名模糊，status 按状态过滤（不传=全部），按下单时间倒序
        String key = (String) params.get("key");
        Object status = params.get("status");
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new LambdaQueryWrapper<OrderEntity>()
                        .and(StringUtils.hasText(key), w -> w
                                .like(OrderEntity::getOrderSn, key)
                                .or().like(OrderEntity::getMemberUsername, key))
                        .eq(status != null && StringUtils.hasText(String.valueOf(status)),
                                OrderEntity::getStatus, status)
                        .orderByDesc(OrderEntity::getCreateTime)
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder(OrderConfirmVo confirmVo) throws ExecutionException, InterruptedException {
        // 构建OrderConfirmVo
        OrderConfirmVo orderConfirmVo = confirmVo == null ? new OrderConfirmVo() : confirmVo;
        // 获取当前用户登录的信息
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        // 获取当前线程请求头信息(解决Feign异步调用丢失请求头问题)
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 开启第一个异步任务
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            // 每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 1、远程查询所有的收获地址列表
            List<MemberAddressVo> address = memberFeignService.getAddress(memberResponseVo.getId());
            orderConfirmVo.setMemberAddressVos(address);
        }, threadPoolExecutor);
        //开启第二个异步任务
        CompletableFuture<Void> cartInfoFuture = CompletableFuture.runAsync(() -> {
            // 每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 2、商品数据：优先使用前端传入的订单项（立即购买直购模式，不读购物车）
            List<OrderItemVo> items = orderConfirmVo.getItems();
            if (items != null && !items.isEmpty()) {
                // 立即购买：补全前端只传了 skuId/count/skuAttrValues 的商品信息
                orderConfirmVo.setItems(items.stream().map(item -> {
                    Result<SkuInfoVo> skuResult = productFeignService.getSkuInfo(item.getSkuId());
                    SkuInfoVo sku = skuResult != null ? skuResult.getData() : null;
                    if (sku == null) {
                        throw new RuntimeException("获取商品信息失败，skuId=" + item.getSkuId());
                    }
                    item.setCheck(true);
                    item.setTitle(sku.getSkuTitle());
                    item.setImage(sku.getSkuDefaultImg());
                    item.setPrice(sku.getPrice());
                    int count = item.getCount() == null ? 1 : item.getCount();
                    item.setCount(count);
                    item.setTotalPrice(sku.getPrice().multiply(new BigDecimal(count)));
                    return item;
                }).collect(Collectors.toList()));
            } else {
                // 购物车结算：远程查询购物车所有选中的购物项
                Result<List<OrderItemVo>> cartResult = cartFeignService.getCurrentCartItems();
                List<OrderItemVo> currentCartItems = cartResult != null ? cartResult.getData() : null;
                orderConfirmVo.setItems(currentCartItems);
            }
        }, threadPoolExecutor).thenRunAsync(() -> {
            List<OrderItemVo> items = orderConfirmVo.getItems();
            // 获取全部商品的id
            List<Long> skuIds = items.stream().map((OrderItemVo::getSkuId)).collect(Collectors.toList());
            // 远程查询商品库存信息（data 为数组，直接反序列化）
            Result<Object> skuHasStock = wmsFeignService.getSkuStock(skuIds);
            List<SkuStockVo> skuStockVos = JSON.parseArray(
                    JSON.toJSONString(skuHasStock == null ? null : skuHasStock.getData()),
                    SkuStockVo.class);
            if (skuStockVos != null && !skuStockVos.isEmpty()) {
                // 将skuStockVos集合转换为map
                Map<Long, Boolean> skuHasStockMap = skuStockVos.stream()
                        .collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                orderConfirmVo.setStocks(skuHasStockMap);
            }
        }, threadPoolExecutor).thenRunAsync(() -> {
            // 补充商家(品牌)信息：结算页按商家分组展示（同一商家商品同一卡片）；一次批量 feign 调用
            List<OrderItemVo> items = orderConfirmVo.getItems();
            if (items == null || items.isEmpty()) {
                return;
            }
            List<Long> skuIds = items.stream()
                    .map(OrderItemVo::getSkuId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (skuIds.isEmpty()) {
                return;
            }
            try {
                Result<Map<Long, SpuInfoVo>> result = productFeignService.getSpuInfoMapBySkuIds(skuIds);
                Map<Long, SpuInfoVo> spuInfoMap = result == null || result.getData() == null
                        ? Collections.emptyMap()
                        : JSON.parseObject(JSON.toJSONString(result.getData()),
                                new TypeReference<Map<Long, SpuInfoVo>>() { });
                for (OrderItemVo item : items) {
                    SpuInfoVo spuInfoData = item.getSkuId() == null ? null : spuInfoMap.get(item.getSkuId());
                    if (spuInfoData != null) {
                        item.setSpuBrand(spuInfoData.getBrandName());
                        item.setBrandId(spuInfoData.getBrandId());
                        item.setBrandLogo(spuInfoData.getBrandLogo());
                    }
                }
            } catch (Exception e) {
                // 品牌信息获取失败不阻塞结算流程
                log.warn("结算单品牌信息批量补全失败: {}", e.getMessage());
            }
        }, threadPoolExecutor);
        // 3、查询用户积分
        Integer integration = memberResponseVo.getIntegration();
        orderConfirmVo.setIntegration(integration);

        // 4、防重令牌(防止表单重复提交)
        // 为用户设置一个token，三十分钟过期时间（存在redis）
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token, 30, TimeUnit.MINUTES);
        orderConfirmVo.setOrderToken(token);

        CompletableFuture.allOf(addressFuture, cartInfoFuture).get();

        // 5、默认地址运费（前端展示用，切换地址时由前端重新获取）
        List<MemberAddressVo> addressList = orderConfirmVo.getMemberAddressVos();
        if (addressList != null && !addressList.isEmpty()) {
            MemberAddressVo defaultAddr = addressList.stream()
                    .filter(a -> a.getDefaultStatus() != null && a.getDefaultStatus() == 1)
                    .findFirst().orElse(addressList.get(0));
            try {
                Result<Object> fareResult = wmsFeignService.getFare(defaultAddr.getId());
                FareVo fareVo = JSON.parseObject(
                        JSON.toJSONString(fareResult == null ? null : fareResult.getData()),
                        FareVo.class);
                if (fareVo != null && fareVo.getFare() != null) {
                    orderConfirmVo.setFreightAmount(fareVo.getFare());
                }
            } catch (Exception e) {
                log.warn("获取默认地址运费失败：" + e.getMessage());
            }
        }

        return orderConfirmVo;
    }

    /**
     * 提交订单
     */
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        confirmVoThreadLocal.set(vo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        // 获取当前用户登录的信息
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        responseVo.setCode(0);
        // 1、验证令牌是否合法【令牌的对比和删除必须保证原子性】
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Arrays.asList(USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()), orderToken);
        if (result == 0L) {
            // 令牌验证失败
            responseVo.setCode(1);
            return responseVo;
        } else {
            // 令牌验证成功
            // 1、创建订单、订单项等信息
            OrderCreateTo order = createOrder();
            // 1.5、应用优惠券（提交时选中）：校验并分摊优惠金额，重算应付
            if (vo.getCouponId() != null) {
                applyCoupon(order, vo.getCouponId(), memberResponseVo.getId());
            }
            // 2、验证价格
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();

            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // 金额对比
                // 3、保存订单
                saveOrder(order);
                // 4、库存锁定,只要有异常，回滚订单数据
                // 订单号、所有订单项信息(skuId,skuNum,skuName)
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                // 收货信息同步到库存工作单（联系人/电话/配送地址/备注/付款方式）
                OrderEntity orderEntity = order.getOrder();
                lockVo.setConsignee(orderEntity.getReceiverName());
                lockVo.setConsigneeTel(orderEntity.getReceiverPhone());
                String deliveryAddress = Stream.of(
                                orderEntity.getReceiverProvince(),
                                orderEntity.getReceiverCity(),
                                orderEntity.getReceiverRegion(),
                                orderEntity.getReceiverDetailAddress())
                        .filter(StringUtils::hasText)
                        .collect(Collectors.joining(""));
                lockVo.setDeliveryAddress(deliveryAddress);
                OrderSubmitVo submitVo = confirmVoThreadLocal.get();
                if (submitVo != null) {
                    lockVo.setOrderComment(submitVo.getRemarks());
                    lockVo.setPaymentWay(submitVo.getPayType());
                }
                // 获取出要锁定的商品数据信息
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map((item) -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(orderItemVos);

                // 调用远程锁定库存的方法
                // 出现的问题：扣减库存成功了，但是由于网络原因超时，出现异常，导致订单事务回滚，库存事务不回滚(解决方案：seata)
                // 为了保证高并发，不推荐使用seata，因为是加锁，并行化，提升不了效率,可以发消息给库存服务
                Result<Object> r = wmsFeignService.orderLockStock(lockVo);
                // 本项目 Result 成功 code 为 200（非原版 0）
                if (r != null && r.getCode() == 200) {
                    // 锁定成功
                    // int i = 1 / 0;
                    // 订单创建成功，发送消息给MQ，使用延时队列实现锁库存
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());

                    // 下单成功即核销优惠券（失败仅告警，不阻断下单）
                    if (vo.getCouponId() != null) {
                        try {
                            CouponUseCheckTo consumeTo = new CouponUseCheckTo();
                            consumeTo.setMemberId(memberResponseVo.getId());
                            consumeTo.setCouponId(vo.getCouponId());
                            consumeTo.setOrderSn(order.getOrder().getOrderSn());
                            couponFeignService.consume(consumeTo);
                        } catch (Exception e) {
                            log.warn("优惠券核销失败，orderSn={}, couponId={}, err={}",
                                    order.getOrder().getOrderSn(), vo.getCouponId(), e.getMessage());
                        }
                    }

                    // 清理购物车中已下单的商品（直购模式不清理：商品未经过购物车，避免误删用户已有购物项）
                    if (vo.getItems() == null || vo.getItems().isEmpty()) {
                        try {
                            List<Long> skuIds = order.getOrderItems().stream()
                                    .map(OrderItemEntity::getSkuId)
                                    .collect(Collectors.toList());
                            cartFeignService.deleteCartItems(skuIds);
                        } catch (Exception e) {
                            log.warn("清理购物车失败，orderSn={}", order.getOrder().getOrderSn(), e);
                        }
                    }

                    responseVo.setOrder(order.getOrder());
                    return responseVo;
                } else {
                    //锁定失败
                    String msg = r.getExtra() != null ? (String) r.getExtra().get("msg") : "未知错误";
                    throw new NoStockException(msg);
                }
            } else {
                responseVo.setCode(2);
                return responseVo;
            }
        }
    }

    /**
     * 按照订单号获取订单信息
     */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {

        return this.baseMapper.selectOne(new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOrderSn, orderSn));
    }

    @Override
    public void cancelOrder(String orderSn) {
        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order == null) {
            throw new RRException("订单不存在");
        }
        // 只能取消自己的订单
        MemberResponseVo member = LoginUserInterceptor.loginUser.get();
        if (member != null && !order.getMemberId().equals(member.getId())) {
            throw new RRException("无权操作该订单");
        }
        // 仅待付款订单可取消，其余状态走售后流程
        if (!OrderStatusEnum.CREATE_NEW.getCode().equals(order.getStatus())) {
            throw new RRException("当前订单状态不可取消");
        }
        // 复用关单逻辑：置为已取消；秒杀订单回补 Redis 库存，普通订单解锁库存
        closeOrder(order);
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        // 关闭订单之前先查询一下数据库，判断此订单状态是否已支付
        OrderEntity orderEntity = this.getById(entity.getId());
        if (orderEntity != null
                && OrderStatusEnum.CREATE_NEW.getCode().equals(orderEntity.getStatus())) {
            // 代付款状态进行关单
            OrderEntity orderUpdate = new OrderEntity();
            orderUpdate.setId(entity.getId());
            orderUpdate.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(orderUpdate);
            // 订单取消/关闭回退优惠券（下单核销的券恢复为未使用；失败仅告警）
            if (orderEntity.getCouponId() != null) {
                try {
                    CouponUseCheckTo refundTo = new CouponUseCheckTo();
                    refundTo.setMemberId(orderEntity.getMemberId());
                    refundTo.setCouponId(orderEntity.getCouponId());
                    refundTo.setOrderSn(orderEntity.getOrderSn());
                    couponFeignService.refund(refundTo);
                } catch (Exception e) {
                    log.warn("优惠券回退失败，orderSn={}, couponId={}, err={}",
                            orderEntity.getOrderSn(), orderEntity.getCouponId(), e.getMessage());
                }
            }
            if (orderEntity.getPromotionSessionId() != null) {
                // 秒杀订单：直接回补 Redis 秒杀库存（信号量 +数量），不触发 ware 解锁
                seckillStockRollback(orderEntity);
                // 释放限购计数（DECR），该用户可按每人限购数量再次抢购
                releaseSeckillPurchaseCount(orderEntity);
            } else {
                // 普通订单：发送消息给MQ，解锁库存
                OrderTo orderTo = new OrderTo();
                BeanUtils.copyProperties(orderEntity, orderTo);
                try {
                    // 确保每个消息发送成功，给每个消息做好日志记录，(给数据库保存每一个详细信息)保存每个消息的详细信息
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
                } catch (Exception e) {
                    // 定期扫描数据库，重新发送失败的消息
                }
            }
        }
    }

    @Override
    public void refundApply(RefundApplyVo vo, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            throw new RRException("请先登录");
        }
        OrderEntity order = getOrderByOrderSn(vo.getOrderSn());
        if (order == null) {
            throw new RRException("订单不存在");
        }
        if (!order.getMemberId().equals(memberId)) {
            throw new RRException("无权操作该订单");
        }
        Integer status = order.getStatus();
        // 已付款（1 待发货 / 2 已发货 / 3 已完成）才可申请退款
        if (status == null || status < 1 || status > 3) {
            throw new RRException("当前订单状态不可申请退款");
        }
        // 同一订单只能有一笔进行中的退款申请（待处理/退货中/已完成）
        long exists = orderReturnApplyService.count(new LambdaQueryWrapper<OrderReturnApplyEntity>()
                .eq(OrderReturnApplyEntity::getOrderSn, vo.getOrderSn())
                .in(OrderReturnApplyEntity::getStatus, 0, 1, 2));
        if (exists > 0) {
            throw new RRException("该订单已提交退款申请，请等待处理");
        }
        // 订单项（取第一条做 sku 信息）
        List<OrderItemEntity> items = orderItemService.list(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderSn, vo.getOrderSn()));
        OrderItemEntity item = (items == null || items.isEmpty()) ? null : items.get(0);

        OrderReturnApplyEntity apply = new OrderReturnApplyEntity();
        apply.setOrderId(order.getId());
        apply.setOrderSn(order.getOrderSn());
        apply.setCreateTime(new Date());
        MemberResponseVo member = LoginUserInterceptor.loginUser.get();
        apply.setMemberUsername(member != null && member.getUsername() != null
                ? member.getUsername()
                : (member != null && member.getNickname() != null ? member.getNickname() : null));
        apply.setReturnAmount(order.getPayAmount());
        apply.setStatus(0);
        apply.setReason(vo.getReason());
        apply.setDescription述(vo.getDescription());
        if (item != null) {
            apply.setSkuId(item.getSkuId());
            apply.setSkuName(item.getSkuName());
            apply.setSkuImg(item.getSkuPic());
            apply.setSkuBrand(item.getSpuBrand());
            apply.setSkuAttrsVals(item.getSkuAttrsVals());
            apply.setSkuCount(item.getSkuQuantity());
            apply.setSkuPrice(item.getSkuPrice());
            apply.setSkuRealPrice(item.getRealAmount());
        }
        orderReturnApplyService.save(apply);
    }

    @Override
    public void receiveOrder(String orderSn, HttpServletRequest request) {
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId == null) {
            throw new RRException("请先登录");
        }
        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order == null) {
            throw new RRException("订单不存在");
        }
        if (!order.getMemberId().equals(memberId)) {
            throw new RRException("无权操作该订单");
        }
        Integer status = order.getStatus();
        // 已付款（1 待发货 / 2 已发货）才可确认收货，直接完成订单
        if (status == null || (status != 1 && status != 2)) {
            throw new RRException("当前订单状态不可确认收货");
        }
        OrderEntity update = new OrderEntity();
        update.setId(order.getId());
        update.setStatus(OrderStatusEnum.RECIEVED.getCode());
        update.setReceiveTime(new Date());
        this.updateById(update);
    }

    @Override
    public OrderReturnApplyEntity getLatestRefundApply(String orderSn) {
        return orderReturnApplyService.getOne(new LambdaQueryWrapper<OrderReturnApplyEntity>()
                .eq(OrderReturnApplyEntity::getOrderSn, orderSn)
                .orderByDesc(OrderReturnApplyEntity::getCreateTime)
                .last("limit 1"));
    }

    @Override
    public long countAfterSaleOrders(Long memberId) {
        return listActiveAfterSaleSns(memberId).size();
    }

    @Override
    public List<String> listActiveAfterSaleSns(Long memberId) {
        if (memberId == null) {
            return Collections.emptyList();
        }
        List<String> mySns = this.list(new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getMemberId, memberId)
                        .select(OrderEntity::getOrderSn))
                .stream()
                .map(OrderEntity::getOrderSn)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (mySns.isEmpty()) {
            return Collections.emptyList();
        }
        return orderReturnApplyService.list(new LambdaQueryWrapper<OrderReturnApplyEntity>()
                        .select(OrderReturnApplyEntity::getOrderSn)
                        .in(OrderReturnApplyEntity::getOrderSn, mySns)
                        .in(OrderReturnApplyEntity::getStatus, 0, 1))
                .stream()
                .map(OrderReturnApplyEntity::getOrderSn)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 确认支付结果：已支付直接返回；否则主动查支付宝交易状态（异步通知丢失/延迟时兜底）
     */
    @Override
    public int confirmPayStatus(String orderSn) {
        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order == null) {
            throw new RRException("订单不存在");
        }
        MemberResponseVo member = LoginUserInterceptor.loginUser.get();
        if (member != null && !order.getMemberId().equals(member.getId())) {
            throw new RRException("无权操作该订单");
        }
        // 已支付直接返回，避免重复查支付宝
        if (OrderStatusEnum.PAYED.getCode().equals(order.getStatus())) {
            return OrderStatusEnum.PAYED.getCode();
        }
        // 主动查单兜底：通知可能丢失，支付宝侧成功则就地更新订单状态
        try {
            String tradeStatus = alipayTemplate.queryTrade(orderSn);
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 订单已关闭（超时/取消）则自动退款，返回取消状态；否则置为已支付
                if (refundIfClosed(orderSn)) {
                    return OrderStatusEnum.CANCLED.getCode();
                }
                this.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode(), PayConstant.ALIPAY);
                return OrderStatusEnum.PAYED.getCode();
            }
            log.info("主动查单未支付: orderSn={}, tradeStatus={}", orderSn, tradeStatus);
        } catch (Exception e) {
            log.warn("主动查单异常: orderSn={}, err={}", orderSn, e.getMessage());
        }
        return order.getStatus();
    }

    /**
     * 订单已关闭（取消/超时）但支付宝侧支付成功时自动退款，退款结果写入 oms_refund_info。
     * 返回 true 表示已按退款处理（调用方不要再置为已支付）；out_request_no 固定订单号保证幂等。
     */
    private boolean refundIfClosed(String orderSn) {
        OrderEntity order = getOrderByOrderSn(orderSn);
        if (order == null || !OrderStatusEnum.CANCLED.getCode().equals(order.getStatus())) {
            return false;
        }
        BigDecimal amount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
        // 落库退款记录（先记处理中，成功后补状态）
        RefundInfoEntity refundInfo = new RefundInfoEntity();
        refundInfo.setOrderReturnId(order.getId());
        refundInfo.setRefund(amount);
        refundInfo.setRefundSn(orderSn);
        refundInfo.setRefundStatus(0);
        refundInfo.setRefundChannel(1);
        refundInfo.setRefundContent("订单已关闭自动退款，订单号：" + orderSn + "，金额：" + amount.toPlainString());
        try {
            String code = alipayTemplate.refund(orderSn, amount);
            if ("10000".equals(code)) {
                refundInfo.setRefundStatus(1);
                log.info("订单已关闭，自动退款成功: orderSn={}, amount={}", orderSn, amount);
            } else {
                refundInfo.setRefundStatus(0);
                refundInfo.setRefundContent(refundInfo.getRefundContent() + "；支付宝返回码：" + code);
                log.warn("订单已关闭，自动退款失败: orderSn={}, alipayCode={}", orderSn, code);
            }
        } catch (Exception e) {
            refundInfo.setRefundStatus(0);
            refundInfo.setRefundContent(refundInfo.getRefundContent() + "；异常：" + e.getMessage());
            log.error("订单已关闭，自动退款异常: orderSn={}", orderSn, e);
        }
        try {
            refundInfoService.save(refundInfo);
        } catch (Exception e) {
            log.error("退款记录落库失败: orderSn={}", orderSn, e);
        }
        return true;
    }

    /**
     * 释放限购计数：取消/超时关单后 DECR 该会员的已抢数量，按每人限购可再次抢购
     */
    private void releaseSeckillPurchaseCount(OrderEntity order) {
        try {
            OrderItemEntity item = orderItemService.getOne(new LambdaQueryWrapper<OrderItemEntity>()
                    .eq(OrderItemEntity::getOrderSn, order.getOrderSn()));
            if (item == null || item.getSkuId() == null) {
                log.warn("释放限购计数：订单 {} 无订单项，跳过", order.getOrderSn());
                return;
            }
            String key = order.getMemberId() + "-" + order.getPromotionSessionId() + "-" + item.getSkuId();
            Long remain = redisTemplate.opsForValue().decrement(key);
            if (remain != null && remain <= 0) {
                redisTemplate.delete(key);
            }
            log.info("取消订单释放限购计数: orderSn={}, key={}, 剩余可抢={}", order.getOrderSn(), key, remain);
        } catch (Exception e) {
            log.error("释放限购计数异常: orderSn={}, err={}", order.getOrderSn(), e.getMessage());
        }
    }

    /**
     * 秒杀建单失败回滚：撤销「已抢购」占位 + 回补 Redis 秒杀库存，用户可重新抢购。
     * <p>best-effort：内部自行兜底，任何异常只记日志，绝不向上抛（调用方需保证 reject 一定执行）。
     */
    @Override
    public void rollbackSeckillStock(SeckillOrderTo orderTo) {
        try {
            String killId = orderTo.getPromotionSessionId() + "-" + orderTo.getSkuId();
            // 1、回滚限购计数（DECR，<=0 清 key），释放该用户的抢购名额
            String countKey = orderTo.getMemberId() + "-" + killId;
            Long remain = redisTemplate.opsForValue().decrement(countKey);
            if (remain != null && remain <= 0) {
                redisTemplate.delete(countKey);
            }
            // 2、从秒杀商品 hash 取 randomCode，回补信号量库存
            Object value = redisTemplate.opsForHash().get("seckill:skus", killId);
            if (value == null) {
                log.warn("秒杀建单失败回滚：killId={} 不在秒杀缓存（可能已被移除关联），跳过库存回补", killId);
                return;
            }
            String randomCode = JSON.parseObject(String.valueOf(value)).getString("randomCode");
            if (StringUtils.hasText(randomCode)) {
                Long restock = redisTemplate.opsForValue()
                        .increment("seckill:stock:" + randomCode, orderTo.getNum());
                log.info("秒杀建单失败回滚：killId={}, 库存+{}，剩余={}", killId, orderTo.getNum(), restock);
            }
        } catch (Exception e) {
            log.error("秒杀建单失败回滚库存异常: orderSn={}, err={}", orderTo.getOrderSn(), e.getMessage());
        }
    }

    /**
     * 秒杀订单超时关单：回补 Redis 秒杀库存（seckill:stock:{randomCode} +num）
     */
    private void seckillStockRollback(OrderEntity order) {
        try {
            OrderItemEntity item = orderItemService.getOne(new LambdaQueryWrapper<OrderItemEntity>()
                    .eq(OrderItemEntity::getOrderSn, order.getOrderSn()));
            if (item == null || item.getSkuId() == null) {
                log.warn("秒杀关单回补：订单 {} 无订单项，跳过", order.getOrderSn());
                return;
            }
            String killId = order.getPromotionSessionId() + "-" + item.getSkuId();
            // 从秒杀商品 hash 中取 randomCode
            Object value = redisTemplate.opsForHash().get("seckill:skus", killId);
            if (value == null) {
                log.warn("秒杀关单回补：killId={} 已不在秒杀缓存（可能已被移除关联），跳过", killId);
                return;
            }
            String randomCode = JSON.parseObject(String.valueOf(value)).getString("randomCode");
            if (randomCode == null || randomCode.isEmpty()) {
                log.warn("秒杀关单回补：killId={} 无 randomCode，跳过", killId);
                return;
            }
            Long restock = redisTemplate.opsForValue()
                    .increment("seckill:stock:" + randomCode, item.getSkuQuantity());
            log.info("秒杀订单超时关单，库存回补: orderSn={}, killId={}, +{}，剩余={}",
                    order.getOrderSn(), killId, item.getSkuQuantity(), restock);
        } catch (Exception e) {
            log.error("秒杀订单超时关单回补库存异常: orderSn={}, err={}", order.getOrderSn(), e.getMessage());
        }
    }

    /**
     * 获取当前订单的支付信息
     */
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity orderInfo = this.getOrderByOrderSn(orderSn);

        // 保留两位小数点，向上取值
        BigDecimal payAmount = orderInfo.getPayAmount().setScale(2, RoundingMode.UP);
        payVo.setTotal_amount(payAmount.toString());
        payVo.setOut_trade_no(orderInfo.getOrderSn());

        // 查询订单项的数据
        List<OrderItemEntity> orderItemInfo = orderItemService.list(
                new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderSn, orderSn));
        OrderItemEntity orderItemEntity = orderItemInfo.get(0);
        payVo.setBody(orderItemEntity.getSkuAttrsVals());
        payVo.setSubject(orderItemEntity.getSkuName());

        return payVo;
    }

    /**
     * 查询当前用户所有订单数据
     */
    @Override
    public boolean hasPaidOrder(Long memberId, Long skuId, Long spuId) {
        if (memberId == null || (skuId == null && spuId == null)) {
            return false;
        }
        // 1. 按 sku/spu 查订单项，取订单号集合（订单项可能未落 order_id，统一用 order_sn 关联）
        LambdaQueryWrapper<OrderItemEntity> itemWrapper = new LambdaQueryWrapper<OrderItemEntity>()
                .select(OrderItemEntity::getOrderSn)
                .eq(skuId != null, OrderItemEntity::getSkuId, skuId)
                .eq(spuId != null, OrderItemEntity::getSpuId, spuId);
        List<OrderItemEntity> items = orderItemService.list(itemWrapper);
        if (items == null || items.isEmpty()) {
            return false;
        }
        List<String> orderSns = items.stream()
                .filter(Objects::nonNull)
                .map(OrderItemEntity::getOrderSn)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (orderSns.isEmpty()) {
            return false;
        }
        // 2. 该会员名下的订单（order_sn 关联，过滤他人订单）
        List<OrderEntity> orders = this.list(new LambdaQueryWrapper<OrderEntity>()
                .in(OrderEntity::getOrderSn, orderSns)
                .eq(OrderEntity::getMemberId, memberId));
        if (orders == null || orders.isEmpty()) {
            return false;
        }
        // 3. 已付款判定：订单状态 1 待发货 / 2 已发货 / 3 已完成（排除 0 待付款 / 4 关闭 / 5 无效）
        boolean statusPaid = orders.stream()
                .anyMatch(o -> o.getStatus() != null && o.getStatus() >= 1 && o.getStatus() <= 3);
        if (statusPaid) {
            return true;
        }
        // 4. 兜底：支付宝异步通知丢失时订单状态可能仍为 0（待付款），
        //    查支付记录（payment_status=TRADE_SUCCESS/TRADE_FINISHED）确认已支付
        List<String> myOrderSns = orders.stream()
                .map(OrderEntity::getOrderSn)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        return paymentInfoService.count(new LambdaQueryWrapper<PaymentInfoEntity>()
                .in(PaymentInfoEntity::getOrderSn, myOrderSns)
                .in(PaymentInfoEntity::getPaymentStatus, "TRADE_SUCCESS", "TRADE_FINISHED")) > 0;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();

        LambdaQueryWrapper<OrderEntity> wrapper = new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getMemberId, memberResponseVo.getId())
                // @TableLogic 逻辑删除会自动追加 delete_status=0，无需手动过滤
                .orderByDesc(OrderEntity::getCreateTime);
        // 订单状态筛选（全部时不传 status）
        Object status = params.get("status");
        if (status != null && StringUtils.hasText(String.valueOf(status))) {
            wrapper.eq(OrderEntity::getStatus, Integer.valueOf(String.valueOf(status)));
            // 进行中的售后（0 待处理 / 1 退货中）只出现在「售后中」tab，不混入其他状态 tab
            List<String> activeAsSns = listActiveAfterSaleSns(memberResponseVo.getId());
            if (!activeAsSns.isEmpty()) {
                wrapper.notIn(OrderEntity::getOrderSn, activeAsSns);
            }
        }
        // 售后中筛选：有进行中的售后申请（0 待处理 / 1 退货中，已完成的退款单不算）
        Object afterSale = params.get("afterSale");
        if (afterSale != null && "1".equals(String.valueOf(afterSale))) {
            List<String> asSns = orderReturnApplyService.list(
                            new LambdaQueryWrapper<OrderReturnApplyEntity>()
                                    .select(OrderReturnApplyEntity::getOrderSn)
                                    .in(OrderReturnApplyEntity::getStatus, 0, 1))
                    .stream()
                    .map(OrderReturnApplyEntity::getOrderSn)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());
            if (asSns.isEmpty()) {
                return new PageUtils(new ArrayList<>(), 0, 10, 1);
            }
            wrapper.in(OrderEntity::getOrderSn, asSns);
        }
        IPage<OrderEntity> page = this.page(new Query<OrderEntity>().getPage(params), wrapper);
        // 遍历所有订单集合
        List<OrderEntity> orderEntityList = page.getRecords().stream().map(order -> {
            // 根据订单号查询订单项里的数据
            List<OrderItemEntity> orderItemEntities = orderItemService.list(new LambdaQueryWrapper<OrderItemEntity>()
                    .eq(OrderItemEntity::getOrderSn, order.getOrderSn()));
            order.setOrderItemEntityList(orderItemEntities);
            return order;
        }).collect(Collectors.toList());

        // 每单最近一笔售后申请状态（批量查询，避免 N+1）
        List<String> orderSns = orderEntityList.stream()
                .map(OrderEntity::getOrderSn)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (!orderSns.isEmpty()) {
            List<OrderReturnApplyEntity> applies = orderReturnApplyService.list(
                    new LambdaQueryWrapper<OrderReturnApplyEntity>()
                            .in(OrderReturnApplyEntity::getOrderSn, orderSns)
                            .orderByDesc(OrderReturnApplyEntity::getCreateTime));
            Map<String, Integer> applyStatusMap = new HashMap<>();
            for (OrderReturnApplyEntity a : applies) {
                if (a.getOrderSn() != null && a.getStatus() != null) {
                    applyStatusMap.putIfAbsent(a.getOrderSn(), a.getStatus());
                }
            }
            orderEntityList.forEach(o -> o.setRefundApplyStatus(applyStatusMap.get(o.getOrderSn())));
        }

        // 批量补全品牌 id/logo/名称（订单列表商家展示用；整页一次 feign 调用，失败不阻塞）
        List<Long> allSkuIds = orderEntityList.stream()
                .flatMap(o -> o.getOrderItemEntityList() == null
                        ? Stream.empty() : o.getOrderItemEntityList().stream())
                .map(OrderItemEntity::getSkuId).filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
        if (!allSkuIds.isEmpty()) {
            try {
                Result<Map<Long, SpuInfoVo>> res = productFeignService.getSpuInfoMapBySkuIds(allSkuIds);
                Map<Long, SpuInfoVo> spuMap = res == null || res.getData() == null ? Collections.emptyMap()
                        : JSON.parseObject(JSON.toJSONString(res.getData()),
                                new TypeReference<Map<Long, SpuInfoVo>>() { });
                if (!spuMap.isEmpty()) {
                    for (OrderEntity o : orderEntityList) {
                        if (o.getOrderItemEntityList() == null) {
                            continue;
                        }
                        for (OrderItemEntity item : o.getOrderItemEntityList()) {
                            SpuInfoVo spuInfoData = item.getSkuId() == null ? null : spuMap.get(item.getSkuId());
                            if (spuInfoData != null) {
                                item.setBrandId(spuInfoData.getBrandId());
                                item.setBrandLogo(spuInfoData.getBrandLogo());
                                if (!StringUtils.hasText(item.getSpuBrand())) {
                                    item.setSpuBrand(spuInfoData.getBrandName());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("订单列表品牌信息批量补全失败: {}", e.getMessage());
            }
        }

        page.setRecords(orderEntityList);

        return new PageUtils(page);
    }

    /**
     * 处理支付宝的支付结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String handlePayResult(PayAsyncVo asyncVo) {
        // 保存交易流水信息
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(asyncVo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(asyncVo.getTrade_no());
        // WAIT_BUYER_PAY 等状态下 buyer_pay_amount 可能为空，需降级处理，避免 NumberFormatException
        String buyerPayAmount = asyncVo.getBuyer_pay_amount();
        String totalAmount = asyncVo.getTotal_amount();
        String amount = StringUtils.hasText(buyerPayAmount) ? buyerPayAmount
                : (StringUtils.hasText(totalAmount) ? totalAmount : "0");
        paymentInfo.setTotalAmount(new BigDecimal(amount));
        paymentInfo.setSubject(asyncVo.getBody());
        paymentInfo.setPaymentStatus(asyncVo.getTrade_status());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setCallbackTime(asyncVo.getNotify_time());
        // 添加到数据库中
        this.paymentInfoService.save(paymentInfo);

        // 修改订单状态
        // 获取当前状态
        String tradeStatus = asyncVo.getTrade_status();

        if (tradeStatus.equals("TRADE_SUCCESS") || tradeStatus.equals("TRADE_FINISHED")) {
            // 支付成功状态
            String orderSn = asyncVo.getOut_trade_no(); // 获取订单号
            // 订单已关闭（超时/取消）则自动退款，不再置为已支付
            if (!refundIfClosed(orderSn)) {
                this.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode(), PayConstant.ALIPAY);
            }
        }

        return "success";
    }

    /**
     * 创建秒杀单（kill 链路同步调用；订单头+订单项同事务，任一失败整体回滚）
     */
    @Override
    @Transactional
    public void createSeckillOrder(SeckillOrderTo orderTo) {
        // 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderTo.getOrderSn());
        orderEntity.setMemberId(orderTo.getMemberId());
        orderEntity.setPromotionSessionId(orderTo.getPromotionSessionId());
        orderEntity.setCreateTime(new Date());
        BigDecimal totalPrice = orderTo.getSeckillPrice().multiply(BigDecimal.valueOf(orderTo.getNum()));
        orderEntity.setPayAmount(totalPrice);
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        // 显式写入逻辑删除标记，避免 @TableLogic 在部分场景下未填充导致查询被过滤
        orderEntity.setDeleteStatus(0);

        // 并行补全订单信息：默认收货地址 + 商品 spu/sku（缩短建单耗时，任一失败不阻塞建单）
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            // 异步线程透传请求上下文，保证 Feign 带上 token 调 mall-member 地址接口
            RequestContextHolder.setRequestAttributes(requestAttributes);
            try {
                List<MemberAddressVo> addresses = memberFeignService.getAddress(orderTo.getMemberId());
                MemberAddressVo address = null;
                if (addresses != null && !addresses.isEmpty()) {
                    // 优先默认地址，无默认取第一条
                    address = addresses.stream()
                            .filter(a -> a.getDefaultStatus() != null && a.getDefaultStatus() == 1)
                            .findFirst().orElse(addresses.get(0));
                }
                if (address != null) {
                    orderEntity.setReceiverName(address.getName());
                    orderEntity.setReceiverPhone(address.getPhone());
                    orderEntity.setReceiverPostCode(address.getPostCode());
                    orderEntity.setReceiverProvince(address.getProvince());
                    orderEntity.setReceiverCity(address.getCity());
                    orderEntity.setReceiverRegion(address.getRegion());
                    orderEntity.setReceiverDetailAddress(address.getDetailAddress());
                } else {
                    log.warn("秒杀建单：会员 {} 无收货地址，收货人字段留空", orderTo.getMemberId());
                }
            } catch (Exception e) {
                log.error("秒杀建单：查询会员 {} 默认地址异常: {}", orderTo.getMemberId(), e.getMessage());
            }
        }, threadPoolExecutor);

        // 保存订单项信息
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderSn(orderTo.getOrderSn());
        orderItem.setSkuId(orderTo.getSkuId());
        orderItem.setRealAmount(totalPrice);
        orderItem.setSkuQuantity(orderTo.getNum());
        // 秒杀订单项补全商品信息：单位价格 = 秒杀价（收银台 单价×数量 = 应付总额）
        orderItem.setSkuPrice(orderTo.getSeckillPrice());

        // 秒杀订单项补全商品信息：spu / sku 并行查询，缩短建单耗时（任一失败不阻塞建单）
        CompletableFuture<Void> skuFuture = CompletableFuture.runAsync(() -> {
            try {
                Result<SkuInfoVo> skuResult = productFeignService.getSkuInfo(orderTo.getSkuId());
                if (skuResult != null && skuResult.getCode() == 200 && skuResult.getData() != null) {
                    SkuInfoVo skuInfo = skuResult.getData();
                    orderItem.setSkuName(StringUtils.hasText(skuInfo.getSkuTitle())
                            ? skuInfo.getSkuTitle() : skuInfo.getSkuName());
                    orderItem.setSkuPic(skuInfo.getSkuDefaultImg());
                } else {
                    log.warn("秒杀建单：查询 sku {} 信息失败，skuName/skuPic 留空", orderTo.getSkuId());
                }
            } catch (Exception e) {
                log.error("秒杀建单：查询 sku {} 信息异常: {}", orderTo.getSkuId(), e.getMessage());
            }
        }, threadPoolExecutor);
        CompletableFuture<Void> spuFuture = CompletableFuture.runAsync(() -> {
            try {
                // data 直接是实体，直接反序列化；查询失败不阻塞建单
                Result<SpuInfoVo> spuInfo = productFeignService.getSpuInfoBySkuId(orderTo.getSkuId());
                if (spuInfo != null && spuInfo.getCode() == 200 && spuInfo.getData() != null) {
                    SpuInfoVo spuInfoData = JSON.parseObject(
                            JSON.toJSONString(spuInfo.getData()), SpuInfoVo.class);
                    if (spuInfoData != null) {
                        orderItem.setSpuId(spuInfoData.getId());
                        orderItem.setSpuName(spuInfoData.getSpuName());
                        orderItem.setSpuBrand(spuInfoData.getBrandName());
                        orderItem.setCategoryId(spuInfoData.getCatalogId());
                    }
                } else {
                    log.warn("秒杀建单：查询 sku {} 的 spu 信息失败，spu 字段留空", orderTo.getSkuId());
                }
            } catch (Exception e) {
                log.error("秒杀建单：查询 sku {} 的 spu 信息异常: {}", orderTo.getSkuId(), e.getMessage());
            }
        }, threadPoolExecutor);
        CompletableFuture<Void> attrsFuture = CompletableFuture.runAsync(() -> {
            try {
                List<String> attrs = productFeignService.getSkuSaleAttrValues(orderTo.getSkuId());
                if (attrs != null && !attrs.isEmpty()) {
                    // 收银台/订单列表按 ; 分隔展示（如 颜色：黑 / 内存：256G）
                    orderItem.setSkuAttrsVals(String.join(";", attrs));
                }
            } catch (Exception e) {
                log.error("秒杀建单：查询 sku {} 销售属性异常: {}", orderTo.getSkuId(), e.getMessage());
            }
        }, threadPoolExecutor);
        CompletableFuture.allOf(addressFuture, skuFuture, spuFuture, attrsFuture).join();

        // 保存订单（含收货人/地址）
        this.save(orderEntity);

        // 保存订单项数据
        orderItemService.save(orderItem);

        // 秒杀订单也走支付超时关单：发送到延迟队列（TTL 5 分钟，死信后关单并回补库存）
        try {
            rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", orderEntity);
        } catch (Exception e) {
            log.error("秒杀订单发送延迟关单消息失败: orderSn={}, err={}", orderTo.getOrderSn(), e.getMessage());
        }
    }

    /**
     * 修改订单状态
     */
    private void updateOrderStatus(String orderSn, Integer code, Integer payType) {

        this.baseMapper.updateOrderStatus(orderSn, code, payType);
    }

    private void saveOrder(OrderCreateTo order) {
        // 获取订单信息
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        orderEntity.setCreateTime(new Date());
        // 保存订单
        this.baseMapper.insert(orderEntity);

        // 获取订单项信息
        List<OrderItemEntity> orderItems = order.getOrderItems();
        // 批量保存订单项数据
        orderItemService.saveBatch(orderItems, 1000);
    }

    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();

        // 1、生成订单号
        String orderSn = UUID.randomUUID().toString().replace("-", "");
        OrderEntity orderEntity = builderOrder(orderSn);
        // 2、获取到所有的订单项
        List<OrderItemEntity> orderItemEntities = builderOrderItems(orderSn);
        // 3、验价(计算价格、积分等信息)
        computePrice(orderEntity, orderItemEntities);
        createTo.setOrder(orderEntity);
        createTo.setOrderItems(orderItemEntities);
        return createTo;
    }

    /**
     * 构建订单数据
     */
    private OrderEntity builderOrder(String orderSn) {
        // 获取当前用户登录信息
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setMemberId(memberResponseVo.getId());
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberUsername(memberResponseVo.getUsername());
        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
        // 远程获取收货地址和运费信息（data 直接是 FareVo 实体，非 Map，直接反序列化）
        Result<Object> fareAddressVo = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = JSON.parseObject(
                JSON.toJSONString(fareAddressVo == null ? null : fareAddressVo.getData()),
                FareVo.class);
        if (fareResp == null) {
            throw new RuntimeException("获取运费/收货地址信息失败，请稍后重试");
        }
        // 获取到运费信息
        BigDecimal fare = fareResp.getFare();
        orderEntity.setFreightAmount(fare);
        // 获取到收货地址信息
        MemberAddressVo address = fareResp.getAddress();
        // 设置收货人信息
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());
        // 设置订单相关的状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);
        orderEntity.setConfirmStatus(0);
        return orderEntity;
    }

    /**
     * 构建所有订单项数据
     */
    public List<OrderItemEntity> builderOrderItems(String orderSn) {

        List<OrderItemEntity> orderItemEntityList = new ArrayList<>();

        // 优先使用前端传入的订单项（立即购买直购模式，不读购物车）；为空则走购物车勾选项
        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
        List<OrderItemVo> submitItems = orderSubmitVo != null ? orderSubmitVo.getItems() : null;
        List<OrderItemVo> currentCartItems = null;
        if (submitItems != null && !submitItems.isEmpty()) {
            // 立即购买直购模式：前端只传 skuId/count/skuAttrValues，价格/标题/图片需补全
            // （与 confirmOrder 结算流程同一套补全逻辑，价格以后端商品服务为准，避免前端传价被篡改）
            currentCartItems = submitItems.stream().map((item) -> {
                if (item.getPrice() != null && item.getTitle() != null) {
                    return item;
                }
                Result<SkuInfoVo> skuResult = productFeignService.getSkuInfo(item.getSkuId());
                SkuInfoVo sku = skuResult != null ? skuResult.getData() : null;
                if (sku == null) {
                    throw new RuntimeException("获取商品信息失败，skuId=" + item.getSkuId());
                }
                item.setCheck(true);
                if (item.getTitle() == null) item.setTitle(sku.getSkuTitle());
                if (item.getImage() == null) item.setImage(sku.getSkuDefaultImg());
                if (item.getPrice() == null) item.setPrice(sku.getPrice());
                if (item.getCount() == null) item.setCount(1);
                item.setTotalPrice(item.getPrice().multiply(new BigDecimal(item.getCount())));
                return item;
            }).collect(Collectors.toList());
        } else {
            // 最后确定每个购物项的价格
            Result<List<OrderItemVo>> cartResult = cartFeignService.getCurrentCartItems();
            currentCartItems = cartResult != null ? cartResult.getData() : null;
        }
        if (currentCartItems != null && !currentCartItems.isEmpty()) {
            orderItemEntityList = currentCartItems.stream().map((items) -> {
                // 构建订单项数据
                OrderItemEntity orderItemEntity = builderOrderItem(items);
                orderItemEntity.setOrderSn(orderSn);

                return orderItemEntity;
            }).collect(Collectors.toList());
        }

        return orderItemEntityList;
    }

    /**
     * 构建某一个订单项的数据
     */
    private OrderItemEntity builderOrderItem(OrderItemVo items) {

        OrderItemEntity orderItemEntity = new OrderItemEntity();

        // 1、商品的spu信息
        Long skuId = items.getSkuId();
        // 获取spu的信息（data 直接是实体，非 Map，直接反序列化）
        Result<SpuInfoVo> spuInfo = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoData = JSON.parseObject(
                JSON.toJSONString(spuInfo == null ? null : spuInfo.getData()),
                SpuInfoVo.class);
        if (spuInfoData == null) {
            throw new RuntimeException("获取商品 SPU 信息失败，skuId=" + skuId);
        }
        orderItemEntity.setSpuId(spuInfoData.getId());
        orderItemEntity.setSpuName(spuInfoData.getSpuName());
        orderItemEntity.setSpuBrand(spuInfoData.getBrandName());
        orderItemEntity.setCategoryId(spuInfoData.getCatalogId());

        // 2、商品的sku信息
        orderItemEntity.setSkuId(skuId);
        orderItemEntity.setSkuName(items.getTitle());
        orderItemEntity.setSkuPic(items.getImage());
        orderItemEntity.setSkuPrice(items.getPrice());
        orderItemEntity.setSkuQuantity(items.getCount());

        // 使用StringUtils.collectionToDelimitedString将list集合转换为String
        String skuAttrValues = StringUtils.collectionToDelimitedString(items.getSkuAttrValues(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttrValues);

        // 3、商品的优惠信息
        // 4、商品的积分信息
        orderItemEntity.setGiftGrowth(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());
        orderItemEntity.setGiftIntegration(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());

        // 5、订单项的价格信息
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);
        // 当前订单项的实际金额.总额 - 各种优惠价格
        // 原来的价格
        BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        // 原价减去优惠价得到最终的价格
        BigDecimal subtract = origin.subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);

        return orderItemEntity;
    }

    /**
     * 应用优惠券：调 coupon 服务校验（已领取未使用/有效/门槛/适用范围），
     * 优惠金额按订单项金额占比分摊，重算订单金额并记录 couponId。
     */
    private void applyCoupon(OrderCreateTo order, Long couponId, Long memberId) {
        OrderEntity orderEntity = order.getOrder();
        List<OrderItemEntity> items = order.getOrderItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        BigDecimal goodsTotal = items.stream()
                .map(OrderItemEntity::getRealAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (goodsTotal.signum() <= 0) {
            return;
        }
        CouponUseCheckTo to = new CouponUseCheckTo();
        to.setMemberId(memberId);
        to.setCouponId(couponId);
        to.setAmount(goodsTotal);
        to.setSkuIds(items.stream()
                .map(OrderItemEntity::getSkuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        Result<BigDecimal> res;
        try {
            res = couponFeignService.useCheck(to);
        } catch (Exception e) {
            throw new RRException("优惠券校验失败，请重试");
        }
        if (res == null || res.getCode() != 200 || res.getData() == null) {
            throw new RRException(res != null && res.getMessage() != null ? res.getMessage() : "优惠券不可用");
        }
        BigDecimal discount = res.getData();
        if (discount.signum() <= 0) {
            return;
        }
        // 优惠金额按订单项金额占比分摊（尾差进最后一项）
        BigDecimal remain = discount;
        for (int i = 0; i < items.size(); i++) {
            OrderItemEntity item = items.get(i);
            BigDecimal share;
            if (i == items.size() - 1) {
                share = remain;
            } else {
                share = discount.multiply(item.getRealAmount())
                        .divide(goodsTotal, 2, RoundingMode.HALF_UP);
                remain = remain.subtract(share);
            }
            item.setCouponAmount(item.getCouponAmount().add(share));
            item.setRealAmount(item.getRealAmount().subtract(share));
        }
        orderEntity.setCouponId(couponId);
        // 重算订单金额（总额/优惠/应付）
        computePrice(orderEntity, items);
    }

    /**
     * 计算价格的方法
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        // 总价
        BigDecimal total = new BigDecimal("0.0");
        // 优惠价
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal intergration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        // 积分、成长值
        Integer integrationTotal = 0;
        Integer growthTotal = 0;
        // 订单总额，叠加每一个订单项的总额信息
        for (OrderItemEntity orderItem : orderItemEntities) {
            // 优惠价格信息
            coupon = coupon.add(orderItem.getCouponAmount());
            promotion = promotion.add(orderItem.getPromotionAmount());
            intergration = intergration.add(orderItem.getIntegrationAmount());
            // 总价
            total = total.add(orderItem.getRealAmount());
            // 积分信息和成长值信息
            integrationTotal += orderItem.getGiftIntegration();
            growthTotal += orderItem.getGiftGrowth();
        }
        // 1、订单价格相关的
        orderEntity.setTotalAmount(total);
        // 设置应付总额(总额+运费，与前端提交的 payPrice 口径一致)
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(intergration);
        // 设置积分成长值信息
        orderEntity.setIntegration(integrationTotal);
        orderEntity.setGrowth(growthTotal);
        // 设置删除状态(0-未删除，1-已删除)
        orderEntity.setDeleteStatus(0);
    }

    /**
     * 补全订单项的品牌 id/名称（店铺跳转用；一次批量 feign 调用，获取失败不阻塞，保留库内 spuBrand）
     */
    @Override
    public void fillItemBrandIds(List<OrderItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> skuIds = items.stream()
                .map(OrderItemEntity::getSkuId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (skuIds.isEmpty()) {
            return;
        }
        try {
            Result<Map<Long, SpuInfoVo>> result = productFeignService.getSpuInfoMapBySkuIds(skuIds);
            Map<Long, SpuInfoVo> spuMap = result == null || result.getData() == null ? Collections.emptyMap()
                    : JSON.parseObject(JSON.toJSONString(result.getData()),
                            new TypeReference<Map<Long, SpuInfoVo>>() { });
            if (spuMap.isEmpty()) {
                return;
            }
            for (OrderItemEntity item : items) {
                SpuInfoVo spuInfoData = item.getSkuId() == null ? null : spuMap.get(item.getSkuId());
                if (spuInfoData != null) {
                    item.setBrandId(spuInfoData.getBrandId());
                    item.setBrandLogo(spuInfoData.getBrandLogo());
                    if (!StringUtils.hasText(item.getSpuBrand())) {
                        item.setSpuBrand(spuInfoData.getBrandName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("订单项品牌信息批量补全失败: " + e.getMessage());
        }
    }
}