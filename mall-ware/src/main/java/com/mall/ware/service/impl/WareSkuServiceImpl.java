package com.mall.ware.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.mall.common.exception.NoStockException;
import com.mall.common.to.mq.OrderTo;
import com.mall.common.to.mq.StockDetailTo;
import com.mall.common.to.mq.StockLockedTo;
import com.mall.common.utils.Result;
import com.mall.common.utils.ResultUtil;
import com.mall.common.vo.WareSkuStockVo;
import com.mall.ware.entity.WareOrderTaskDetailEntity;
import com.mall.ware.entity.WareOrderTaskEntity;
import com.mall.ware.feign.OrderFeignService;
import com.mall.ware.feign.ProductFeignService;
import com.mall.ware.service.WareOrderTaskDetailService;
import com.mall.ware.service.WareOrderTaskService;
import com.mall.ware.vo.*;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.ware.dao.WareSkuDao;
import com.mall.ware.entity.WareSkuEntity;
import com.mall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("wareSkuService")
@RequiredArgsConstructor
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    private final WareSkuDao wareSkuDao;


    private final ProductFeignService productFeignService;


    private final RabbitTemplate rabbitTemplate;


    private final WareOrderTaskService wareOrderTaskService;


    private final WareOrderTaskDetailService wareOrderTaskDetailService;


    private final OrderFeignService orderFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<WareSkuEntity> queryWrapper = new LambdaQueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq(WareSkuEntity::getSkuId, skuId);
        }

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq(WareSkuEntity::getWareId, wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params), queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 1、判断如果还没有这个库存记录新增
        List<WareSkuEntity> entities = wareSkuDao.selectList(new LambdaQueryWrapper<WareSkuEntity>().eq(WareSkuEntity::getSkuId, skuId).eq(WareSkuEntity::getWareId, wareId));
        if (entities == null || entities.isEmpty()) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            // 远程查询sku的名字，如果失败，整个事务无需回滚
            try {
                Result<Object> info = productFeignService.info(skuId);
                // Result 成功码为 200，数据在 data（SkuInfoEntity 序列化后的 Map）
                if (info.getCode() == 200 && info.getData() instanceof Map) {
                    Object name = ((Map<?, ?>) info.getData()).get("skuName");
                    if (name != null) {
                        skuEntity.setSkuName(name.toString());
                    } else {
                        log.warn("addStock拉取SKU名称失败：skuId=" + skuId + " 无 skuName");
                    }
                } else {
                    log.warn("addStock拉取SKU名称失败：skuId=" + skuId + " code=" + info.getCode());
                }
            } catch (Exception e) {
                log.warn("addStock拉取SKU名称异常：skuId=" + skuId + "，" + e);
            }
            wareSkuDao.insert(skuEntity);
        } else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    /**
     * 设置 SKU 库存（发布商品/库存管理用，SET 语义）：
     * 已有记录则覆盖库存数（不动锁定库存），没有则新增
     */
    @Override
    public void saveStock(List<WareSkuStockVo> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        for (WareSkuStockVo vo : vos) {
            if (vo.getSkuId() == null || vo.getStock() == null) {
                continue;
            }
            if (vo.getWareId() == null) {
                log.warn("保存SKU库存跳过：skuId=" + vo.getSkuId() + " 未指定仓库");
                continue;
            }
            WareSkuEntity exist = wareSkuDao.selectOne(new LambdaQueryWrapper<WareSkuEntity>()
                    .eq(WareSkuEntity::getSkuId, vo.getSkuId())
                    .eq(WareSkuEntity::getWareId, vo.getWareId()));
            if (exist == null) {
                WareSkuEntity entity = new WareSkuEntity();
                entity.setSkuId(vo.getSkuId());
                entity.setWareId(vo.getWareId());
                entity.setStock(vo.getStock());
                entity.setStockLocked(0);
                fillSkuName(entity);
                wareSkuDao.insert(entity);
            } else {
                exist.setStock(vo.getStock());
                wareSkuDao.updateById(exist);
            }
        }
    }

    /**
     * 从商品服务拉取 skuName 填充（失败打日志，不影响主流程）
     */
    private void fillSkuName(WareSkuEntity entity) {
        try {
            Result<Object> info = productFeignService.info(entity.getSkuId());
            if (info.getCode() == 200 && info.getData() instanceof Map) {
                Object name = ((Map<?, ?>) info.getData()).get("skuName");
                if (name != null) {
                    entity.setSkuName(name.toString());
                } else {
                    log.warn("拉取SKU名称失败：skuId=" + entity.getSkuId() + " 返回数据中无 skuName");
                }
            } else {
                log.warn("拉取SKU名称失败：skuId=" + entity.getSkuId() + " code=" + info.getCode() + " data类型=" + (info.getData() == null ? "null" : info.getData().getClass().getName()));
            }
        } catch (Exception e) {
            log.warn("拉取SKU名称异常：skuId=" + entity.getSkuId() + "，" + e);
        }
    }

    /**
     * 保存/修改时 skuName 为空则自动从商品服务补齐，保证库存表 sku_name 不落空
     */
    @Override
    public boolean save(WareSkuEntity entity) {
        if (entity.getSkuId() != null && StringUtils.isEmpty(entity.getSkuName())) {
            fillSkuName(entity);
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(WareSkuEntity entity) {
        if (entity.getSkuId() != null && StringUtils.isEmpty(entity.getSkuName())) {
            fillSkuName(entity);
        }
        return super.updateById(entity);
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
        return skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            // 查询当前sku的总库存量
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count != null && count > 0);
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        /**
         * 保存库存工作单详情信息
         * 追溯
         */
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskEntity.setCreateTime(new Date());
        // 同步下单收货信息（联系人/电话/配送地址/备注/付款方式）
        wareOrderTaskEntity.setConsignee(vo.getConsignee());
        wareOrderTaskEntity.setConsigneeTel(vo.getConsigneeTel());
        wareOrderTaskEntity.setDeliveryAddress(vo.getDeliveryAddress());
        wareOrderTaskEntity.setOrderComment(vo.getOrderComment());
        wareOrderTaskEntity.setPaymentWay(vo.getPaymentWay());
        wareOrderTaskService.save(wareOrderTaskEntity);

        // 1、按照下单收货地址，找到一个就近仓库，锁定库存
        // 2、找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map((item) -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            // 查询这个商品在哪个仓库有库存
            List<Long> wareIdList = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIdList);
            return stock;
        }).collect(Collectors.toList());
        // 3、锁定库存
        for (SkuWareHasStock hasStock : collect) {
            boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            log.info("[orderLockStock] skuId={}, num={}, wareIds={}", skuId, hasStock.getNum(), wareIds);

            if (wareIds == null || wareIds.isEmpty()) {
                //没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }
            // 3.1、如果每一个商品都锁定成功,将当前商品锁定了几件的工作单记录发给MQ
            // 3.2、锁定失败,前面保存的工作单信息都回滚了。发送出去的消息，即使要解锁库存，由于在数据库查不到指定的id，所有就不用解锁
            for (Long wareId : wareIds) {
                // 锁定成功就返回1，失败就返回0
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                log.info("[orderLockStock] lock skuId={} wareId={} count={}", skuId, wareId, count);
                if (count == 1) {
                    skuStocked = true;

                    WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity(null, skuId, "",
                            hasStock.getNum(), wareOrderTaskEntity.getId(), wareId, 1);
                    wareOrderTaskDetailService.save(entity);

                    // 告诉MQ库存锁定成功
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo detailTo = new StockDetailTo();
                    BeanUtils.copyProperties(entity, detailTo);
                    lockedTo.setDetailTo(detailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                    break;
                } else {
                    // 当前仓库锁失败，重试下一个仓库
                }
            }
            if (!skuStocked) {
                // 当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }
        // 4、肯定全部都是锁定成功的
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {
        System.out.println("收到解锁库存的消息");
        StockDetailTo detail = to.getDetailTo();
        Long detailId = detail.getId();
        // 解锁
        // 1、查询数据库关于这个订单的锁定库存信息
        // 有库存：库存锁定成功，需要解锁
        // 1.1、解锁：订单情况
        // 没有这个订单，必须解锁
        // 有这个订单，不是解锁库存
        //     订单状态：已取消，解锁库存
        //             没取消，不能解锁
        // 没有库存：库存锁定失败，库存回滚了，无需解锁
        WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailId);
        if (byId != null) {
            // 解锁
            Long id = to.getId();
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            // 远程查询订单信息
            Result<Object> orderData = orderFeignService.getOrderStatus(orderSn);
            // 本项目 Result 成功 code 为 200（非原版 0）
            if (orderData != null && orderData.getCode() == 200) {
                // 订单数据返回成功（data 直接是实体，直接反序列化）
                OrderVo orderInfo = JSON.parseObject(
                        JSON.toJSONString(orderData == null ? null : orderData.getData()),
                        OrderVo.class);

                // 判断订单状态是否已取消或者支付或者订单不存在
                if (orderInfo == null || orderInfo.getStatus() == 4) {
                    // 订单已被取消，才能解锁库存
                    if (byId.getLockStatus() == 1) {
                        // 当前库存工作单详情状态1，已锁定，但是未解锁才可以解锁
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                }
            } else {
                // 消息拒绝以后重新放在队列里面，让别人继续消费解锁
                // 远程调用服务失败
                throw new RuntimeException("远程调用服务失败");
            }
        } else {
            // 无需解锁
        }
    }

    /**
     * 防止订单服务卡顿，导致订单状态消息一直改不了，库存优先到期，查订单状态新建，什么都不处理
     * 导致卡顿的订单，永远都不能解锁库存
     */
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        // 查一下最新的库存解锁状态，防止重复解锁库存
        WareOrderTaskEntity orderTaskEntity = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);

        // 按照工作单的id找到所有 没有解锁的库存，进行解锁
        Long id = orderTaskEntity.getId();
        List<WareOrderTaskDetailEntity> list = wareOrderTaskDetailService.list(new LambdaQueryWrapper<WareOrderTaskDetailEntity>()
                .eq(WareOrderTaskDetailEntity::getTaskId, id).eq(WareOrderTaskDetailEntity::getLockStatus, 1));
        for (WareOrderTaskDetailEntity taskDetailEntity : list) {
            unLockStock(taskDetailEntity.getSkuId(),
                    taskDetailEntity.getWareId(),
                    taskDetailEntity.getSkuNum(),
                    taskDetailEntity.getId());
        }
    }

    private void unLockStock(Long skuId, Long wareId, Integer skuNum, Long taskDetailId) {
        // 库存解锁
        wareSkuDao.unLockStock(skuId, wareId, skuNum);
        // 更新工作单的状态
        WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity();
        taskDetailEntity.setId(taskDetailId);
        // 变为已解锁
        taskDetailEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(taskDetailEntity);
    }
}