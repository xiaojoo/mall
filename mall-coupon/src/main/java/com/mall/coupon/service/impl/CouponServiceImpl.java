package com.mall.coupon.service.impl;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.exception.RRException;
import com.mall.common.to.CouponUseCheckTo;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import com.mall.common.utils.Result;
import lombok.RequiredArgsConstructor;

import com.mall.coupon.dao.CouponDao;
import com.mall.coupon.entity.CouponEntity;
import com.mall.coupon.entity.CouponHistoryEntity;
import com.mall.coupon.entity.CouponSpuRelationEntity;
import com.mall.coupon.entity.CouponSpuCategoryRelationEntity;
import com.mall.coupon.feign.ProductFeignService;
import com.mall.coupon.service.CouponHistoryService;
import com.mall.coupon.service.CouponService;
import com.mall.coupon.service.CouponSpuRelationService;
import com.mall.coupon.service.CouponSpuCategoryRelationService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service("couponService")
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponDao, CouponEntity> implements CouponService {

    private final CouponHistoryService couponHistoryService;

    private final CouponSpuRelationService couponSpuRelationService;

    private final CouponSpuCategoryRelationService couponSpuCategoryRelationService;

    private final ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        IPage<CouponEntity> page = this.page(
                new Query<CouponEntity>().getPage(params),
                new LambdaQueryWrapper<CouponEntity>()
        .like(StringUtils.isNotBlank(key), CouponEntity::getCouponName, key)
        );

        return new PageUtils(page);
    }

    /**
     * 管理端-可使用优惠券列表（已发布且在有效时间窗口内）；
     * 传 skuId 时按适用范围过滤：全场通用 / 指定分类（含祖先分类链）/ 指定商品
     */
    @Override
    public List<CouponEntity> listUsable(Long skuId) {
        List<CouponEntity> coupons = getMemberCouponList(null);
        if (skuId == null || coupons.isEmpty()) {
            return coupons;
        }
        // 指定商品关联：couponId -> spuId 列表
        Map<Long, Set<Long>> spuRelMap = couponSpuRelationService.list().stream()
                .filter(r -> r.getCouponId() != null && r.getSpuId() != null)
                .collect(Collectors.groupingBy(CouponSpuRelationEntity::getCouponId,
                        Collectors.mapping(CouponSpuRelationEntity::getSpuId, Collectors.toSet())));
        // 指定分类关联：couponId -> categoryId 列表
        Map<Long, Set<Long>> catRelMap = couponSpuCategoryRelationService.list().stream()
                .filter(r -> r.getCouponId() != null && r.getCategoryId() != null)
                .collect(Collectors.groupingBy(CouponSpuCategoryRelationEntity::getCouponId,
                        Collectors.mapping(CouponSpuCategoryRelationEntity::getCategoryId, Collectors.toSet())));
        // 当前 sku 的叶子分类 + 祖先分类链路
        Set<Long> catChain = buildCategoryChain(skuId);
        // 当前 sku 所属店铺（品牌）
        Long skuBrandId = getSkuBrandId(skuId);
        log.info("可用优惠券过滤 skuId={}, 分类链路={}, 品牌={}, 券总数={}, 指定商品关联券数={}, 指定分类关联券数={}",
                skuId, catChain, skuBrandId, coupons.size(), spuRelMap.size(), catRelMap.size());

        return coupons.stream().filter(coupon -> {
            // 店铺券：仅该店铺（品牌）的商品可用
            if (coupon.getBrandId() != null && !coupon.getBrandId().equals(skuBrandId)) {
                log.info("券 id={}（店铺{}）不适用于 skuId={}（品牌{}）",
                        coupon.getId(), coupon.getBrandId(), skuId, skuBrandId);
                return false;
            }
            int type = coupon.getUseType() == null ? 0 : coupon.getUseType();
            if (type == 0) {
                return true; // 全场通用
            }
            if (type == 2) { // 指定商品：关联表包含该 skuId
                Set<Long> spuIds = spuRelMap.get(coupon.getId());
                boolean hit = spuIds != null && spuIds.contains(skuId);
                if (!hit) {
                    log.info("券 id={}（指定商品）未命中 skuId={}", coupon.getId(), skuId);
                }
                return hit;
            }
            if (type == 1) { // 指定分类：关联分类命中 sku 分类或其祖先
                if (catChain.isEmpty()) {
                    log.info("券 id={}（指定分类）未命中：分类链路为空", coupon.getId());
                    return false;
                }
                Set<Long> catIds = catRelMap.get(coupon.getId());
                boolean hit = catIds != null && catIds.stream().anyMatch(catChain::contains);
                if (!hit) {
                    log.info("券 id={}（指定分类）未命中：关联分类={}, sku链路={}",
                            coupon.getId(), catIds, catChain);
                }
                return hit;
            }
            return false;
        }).collect(Collectors.toList());
    }

    /**
     * 查 sku 的叶子分类（catalogId）并向上收集祖先分类 id（含自身）
     */
    @Override
    public List<CouponEntity> listMemberUsable(Long memberId, BigDecimal amount, List<Long> skuIds) {
        if (memberId == null) {
            return new ArrayList<>();
        }
        Date now = new Date();
        // 1. 该会员已领取且未使用的券
        List<CouponHistoryEntity> history = couponHistoryService.list(
                new LambdaQueryWrapper<CouponHistoryEntity>()
                        .eq(CouponHistoryEntity::getMemberId, memberId)
                        .eq(CouponHistoryEntity::getUseType, 0));
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> couponIds = history.stream()
                .map(CouponHistoryEntity::getCouponId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (couponIds.isEmpty()) {
            return new ArrayList<>();
        }
        // 2. 券有效 + 使用门槛满足（min_point <= 商品总额；null 视为无门槛/无时间限制）
        BigDecimal total = amount == null ? BigDecimal.ZERO : amount;
        List<CouponEntity> coupons = this.list(
                new LambdaQueryWrapper<CouponEntity>()
                        .in(CouponEntity::getId, couponIds)
                        .eq(CouponEntity::getPublish, 1)
                        .and(w -> w.isNull(CouponEntity::getMinPoint)
                                .or().le(CouponEntity::getMinPoint, total))
                        .and(w -> w.isNull(CouponEntity::getEnableStartTime)
                                .or().le(CouponEntity::getEnableStartTime, now))
                        .and(w -> w.isNull(CouponEntity::getEnableEndTime)
                                .or().ge(CouponEntity::getEnableEndTime, now)));
        if (coupons.isEmpty() || skuIds == null || skuIds.isEmpty()) {
            return coupons == null ? new ArrayList<>() : coupons;
        }
        // 3. 适用范围过滤（多 sku 任一命中即可）：全场通用 / 指定商品 / 指定分类（含祖先）
        Map<Long, Set<Long>> spuRelMap = couponSpuRelationService.list().stream()
                .filter(r -> r.getCouponId() != null && r.getSpuId() != null)
                .collect(Collectors.groupingBy(CouponSpuRelationEntity::getCouponId,
                        Collectors.mapping(CouponSpuRelationEntity::getSpuId, Collectors.toSet())));
        Map<Long, Set<Long>> catRelMap = couponSpuCategoryRelationService.list().stream()
                .filter(r -> r.getCouponId() != null && r.getCategoryId() != null)
                .collect(Collectors.groupingBy(CouponSpuCategoryRelationEntity::getCouponId,
                        Collectors.mapping(CouponSpuCategoryRelationEntity::getCategoryId, Collectors.toSet())));
        Set<Long> skuSet = new HashSet<>(skuIds);
        // 全部 sku 的叶子分类 + 祖先分类链路合并
        Set<Long> catChain = new HashSet<>();
        for (Long skuId : skuIds) {
            catChain.addAll(buildCategoryChain(skuId));
        }
        // 订单商品所属店铺（品牌）集合：店铺券必须命中
        Set<Long> skuBrandSet = new HashSet<>();
        for (Long skuId : skuIds) {
            Long brandId = getSkuBrandId(skuId);
            if (brandId != null) {
                skuBrandSet.add(brandId);
            }
        }
        return coupons.stream().filter(coupon -> {
            // 店铺券：订单必须包含该店铺（品牌）的商品
            if (coupon.getBrandId() != null && !skuBrandSet.contains(coupon.getBrandId())) {
                log.info("券 id={}（店铺{}）不适用于当前订单商品", coupon.getId(), coupon.getBrandId());
                return false;
            }
            int type = coupon.getUseType() == null ? 0 : coupon.getUseType();
            if (type == 0) {
                return true;
            }
            if (type == 2) {
                Set<Long> spuIds = spuRelMap.get(coupon.getId());
                return spuIds != null && spuIds.stream().anyMatch(skuSet::contains);
            }
            if (type == 1) {
                if (catChain.isEmpty()) {
                    return false;
                }
                Set<Long> catIds = catRelMap.get(coupon.getId());
                return catIds != null && catIds.stream().anyMatch(catChain::contains);
            }
            return false;
        }).collect(Collectors.toList());
    }

    /**
     * C端-优惠券使用校验（订单提交时 order 服务调用）：
     * 已领取未使用 + 券有效 + 门槛满足 + 适用范围匹配，通过返回优惠金额，否则抛 RRException
     */
    @Override
    public BigDecimal useCheckCoupon(CouponUseCheckTo to) {
        if (to == null || to.getMemberId() == null || to.getCouponId() == null) {
            throw new RRException("优惠券参数缺失");
        }
        Date now = new Date();
        // 1. 该会员已领取且未使用
        long cnt = couponHistoryService.count(new LambdaQueryWrapper<CouponHistoryEntity>()
                .eq(CouponHistoryEntity::getMemberId, to.getMemberId())
                .eq(CouponHistoryEntity::getCouponId, to.getCouponId())
                .eq(CouponHistoryEntity::getUseType, 0));
        if (cnt <= 0) {
            throw new RRException("优惠券未领取或已使用");
        }
        // 2. 券有效（发布 + 时间窗口）
        CouponEntity coupon = this.getById(to.getCouponId());
        if (coupon == null || !Integer.valueOf(1).equals(coupon.getPublish())) {
            throw new RRException("优惠券不可用");
        }
        if (coupon.getEnableStartTime() != null && coupon.getEnableStartTime().after(now)) {
            throw new RRException("优惠券未到使用时间");
        }
        if (coupon.getEnableEndTime() != null && coupon.getEnableEndTime().before(now)) {
            throw new RRException("优惠券已过期");
        }
        // 3. 门槛满足
        BigDecimal amount = to.getAmount() == null ? BigDecimal.ZERO : to.getAmount();
        if (coupon.getMinPoint() != null && coupon.getMinPoint().compareTo(amount) > 0) {
            throw new RRException("未满足优惠券使用门槛");
        }
        // 4. 适用范围匹配（全场/指定分类含祖先/指定商品，多 sku 任一命中）
        List<Long> skuIds = to.getSkuIds();
        if (skuIds != null && !skuIds.isEmpty() && !isCouponApplicable(coupon, skuIds)) {
            throw new RRException("优惠券不适用于当前商品");
        }
        // 4.5 店铺券：订单商品必须包含该店铺（品牌）的商品（查询失败视为不命中，fail-closed）
        if (coupon.getBrandId() != null) {
            boolean hasBrandSku = skuIds != null && !skuIds.isEmpty() && skuIds.stream()
                    .anyMatch(skuId -> Objects.equals(getSkuBrandId(skuId), coupon.getBrandId()));
            if (!hasBrandSku) {
                throw new RRException("优惠券仅限该店铺商品使用");
            }
        }
        // 优惠金额不超过商品总额
        BigDecimal discount = coupon.getAmount() == null ? BigDecimal.ZERO : coupon.getAmount();
        return discount.min(amount);
    }

    /**
     * 查询 sku 所属店铺（品牌）id（店铺券判断用；查询失败返回 null）
     */
    private Long getSkuBrandId(Long skuId) {
        if (skuId == null) {
            return null;
        }
        try {
            Result<Object> skuRes = productFeignService.getSkuInfo(skuId);
            if (skuRes.getCode() != 200 || skuRes.getData() == null) {
                return null;
            }
            JSONObject sku = JSON.parseObject(JSON.toJSONString(skuRes.getData()));
            return sku.getLong("brandId");
        } catch (Exception e) {
            log.warn("查询 sku {} 品牌失败: {}", skuId, e.getMessage());
            return null;
        }
    }

    /**
     * 券适用范围是否命中任一 sku（全场通用 useType=0 / 指定商品 useType=2 / 指定分类 useType=1 含祖先）
     */
    private boolean isCouponApplicable(CouponEntity coupon, List<Long> skuIds) {
        int type = coupon.getUseType() == null ? 0 : coupon.getUseType();
        if (type == 0) {
            return true;
        }
        if (type == 2) {
            Map<Long, Set<Long>> spuRelMap = couponSpuRelationService.list().stream()
                    .filter(r -> r.getCouponId() != null && r.getSpuId() != null)
                    .collect(Collectors.groupingBy(CouponSpuRelationEntity::getCouponId,
                            Collectors.mapping(CouponSpuRelationEntity::getSpuId, Collectors.toSet())));
            Set<Long> spuIds = spuRelMap.get(coupon.getId());
            return spuIds != null && skuIds.stream().anyMatch(spuIds::contains);
        }
        if (type == 1) {
            Map<Long, Set<Long>> catRelMap = couponSpuCategoryRelationService.list().stream()
                    .filter(r -> r.getCouponId() != null && r.getCategoryId() != null)
                    .collect(Collectors.groupingBy(CouponSpuCategoryRelationEntity::getCouponId,
                            Collectors.mapping(CouponSpuCategoryRelationEntity::getCategoryId, Collectors.toSet())));
            Set<Long> catIds = catRelMap.get(coupon.getId());
            if (catIds == null || catIds.isEmpty()) {
                return false;
            }
            Set<Long> catChain = new HashSet<>();
            for (Long skuId : skuIds) {
                catChain.addAll(buildCategoryChain(skuId));
            }
            return catChain.stream().anyMatch(catIds::contains);
        }
        return false;
    }

    /**
     * 下单成功核销：未使用记录标记已使用（use_type=1），回写订单号；找不到未使用记录仅告警不报错
     */
    @Override
    @Transactional
    public void consumeCoupon(Long memberId, Long couponId, String orderSn) {
        if (memberId == null || couponId == null) {
            return;
        }
        boolean updated = couponHistoryService.update(new LambdaUpdateWrapper<CouponHistoryEntity>()
                .eq(CouponHistoryEntity::getMemberId, memberId)
                .eq(CouponHistoryEntity::getCouponId, couponId)
                .eq(CouponHistoryEntity::getUseType, 0)
                .set(CouponHistoryEntity::getUseType, 1)
                .set(CouponHistoryEntity::getUseTime, new Date())
                .set(StringUtils.isNotBlank(orderSn), CouponHistoryEntity::getOrderSn, orderSn));
        if (!updated) {
            log.warn("优惠券核销未命中未使用记录: memberId={}, couponId={}, orderSn={}", memberId, couponId, orderSn);
        }
    }

    /**
     * 订单取消/关闭回退：已使用记录（匹配订单号）回退为未使用，清空使用时间和订单号；未命中仅告警
     */
    @Override
    @Transactional
    public void refundCoupon(Long memberId, Long couponId, String orderSn) {
        if (memberId == null || couponId == null) {
            return;
        }
        boolean updated = couponHistoryService.update(new LambdaUpdateWrapper<CouponHistoryEntity>()
                .eq(CouponHistoryEntity::getMemberId, memberId)
                .eq(CouponHistoryEntity::getCouponId, couponId)
                .eq(CouponHistoryEntity::getUseType, 1)
                .eq(StringUtils.isNotBlank(orderSn), CouponHistoryEntity::getOrderSn, orderSn)
                .set(CouponHistoryEntity::getUseType, 0)
                .set(CouponHistoryEntity::getUseTime, null)
                .set(CouponHistoryEntity::getOrderSn, null));
        if (!updated) {
            log.warn("优惠券回退未命中已使用记录: memberId={}, couponId={}, orderSn={}", memberId, couponId, orderSn);
        }
    }

    private Set<Long> buildCategoryChain(Long skuId) {
        Set<Long> chain = new HashSet<>();
        try {
            Result<Object> skuRes = productFeignService.getSkuInfo(skuId);
            if (skuRes.getCode() != 200 || skuRes.getData() == null) {
                return chain;
            }
            JSONObject sku = JSON.parseObject(JSON.toJSONString(skuRes.getData()));
            Long catalogId = sku.getLong("catalogId");
            log.info("skuInfo 结果: code={}, catalogId={}", skuRes.getCode(), catalogId);
            if (catalogId == null) {
                return chain;
            }
            chain.add(catalogId);

            // 分类树：catId -> parentCid 映射，向上回溯
            Result<Object> treeRes = productFeignService.getCategoryTree();
            log.info("分类树结果: code={}, data为空={}", treeRes.getCode(), treeRes.getData() == null);
            if (treeRes.getCode() == 200 && treeRes.getData() != null) {
                Map<String, String> parentMap = new HashMap<>();
                JSONArray tree = JSON.parseArray(JSON.toJSONString(treeRes.getData()));
                collectParentMap(tree, parentMap);
                String cur = String.valueOf(catalogId);
                int guard = 0;
                while (guard++ < 10) {
                    String parent = parentMap.get(cur);
                    if (parent == null || "0".equals(parent)) {
                        break;
                    }
                    chain.add(Long.parseLong(parent));
                    cur = parent;
                }
            }
        } catch (Exception e) {
            log.warn("构建 sku {} 分类链路失败: {}", skuId, e.getMessage());
        }
        return chain;
    }

    private void collectParentMap(JSONArray nodes, Map<String, String> parentMap) {
        if (nodes == null) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            String catId = String.valueOf(node.getLong("catId"));
            String parentCid = String.valueOf(node.getLong("parentCid"));
            if (node.getLong("parentCid") != null) {
                parentMap.put(catId, parentCid);
            }
            JSONArray children = node.getJSONArray("children");
            if (children != null) {
                collectParentMap(children, parentMap);
            }
        }
    }

    @Override
    public List<CouponEntity> getMemberCouponList(Long memberId) {
        Date now = new Date();
        List<CouponEntity> list = this.list(new LambdaQueryWrapper<CouponEntity>()
                .eq(CouponEntity::getPublish, 1)
                .and(w -> w.isNull(CouponEntity::getEnableStartTime)
                        .or().le(CouponEntity::getEnableStartTime, now))
                .and(w -> w.isNull(CouponEntity::getEnableEndTime)
                        .or().ge(CouponEntity::getEnableEndTime, now))
                .orderByDesc(CouponEntity::getEndTime));
        // 标记当前会员已领取的券
        markClaimed(list, memberId);
        return list;
    }

    @Override
    public List<CouponEntity> getShopCouponList(Long brandId, Long memberId) {
        if (brandId == null) {
            return Collections.emptyList();
        }
        Date now = new Date();
        List<CouponEntity> list = this.list(new LambdaQueryWrapper<CouponEntity>()
                .eq(CouponEntity::getPublish, 1)
                .eq(CouponEntity::getBrandId, brandId)
                .and(w -> w.isNull(CouponEntity::getEnableStartTime)
                        .or().le(CouponEntity::getEnableStartTime, now))
                .and(w -> w.isNull(CouponEntity::getEnableEndTime)
                        .or().ge(CouponEntity::getEnableEndTime, now))
                .orderByDesc(CouponEntity::getEndTime));
        markClaimed(list, memberId);
        return list;
    }

    /**
     * 批量标记当前会员已领取的券（非表字段 claimed）
     */
    private void markClaimed(List<CouponEntity> list, Long memberId) {
        if (memberId == null || list == null || list.isEmpty()) {
            return;
        }
        Set<Long> claimedIds = couponHistoryService.list(
                        new LambdaQueryWrapper<CouponHistoryEntity>()
                                .eq(CouponHistoryEntity::getMemberId, memberId)
                                .in(CouponHistoryEntity::getCouponId,
                                        list.stream().map(CouponEntity::getId).collect(Collectors.toList())))
                .stream().map(CouponHistoryEntity::getCouponId).collect(Collectors.toSet());
        list.forEach(c -> c.setClaimed(claimedIds.contains(c.getId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveCoupon(Long memberId, Long couponId) {
        if (memberId == null || couponId == null) {
            throw new RRException("参数错误");
        }
        CouponEntity coupon = this.getById(couponId);
        if (coupon == null) {
            throw new RRException("优惠券不存在");
        }
        Date now = new Date();
        // 发布状态
        if (coupon.getPublish() == null || coupon.getPublish() != 1) {
            throw new RRException("优惠券未发布");
        }
        // 领取时间窗口
        if (coupon.getEnableStartTime() != null && now.before(coupon.getEnableStartTime())) {
            throw new RRException("优惠券还未到领取时间");
        }
        if (coupon.getEnableEndTime() != null && now.after(coupon.getEnableEndTime())) {
            throw new RRException("优惠券已过领取时间");
        }
        // 库存：发行数量（兼容 num 字段）
        int total = coupon.getPublishCount() != null ? coupon.getPublishCount()
                : (coupon.getNum() != null ? coupon.getNum() : 0);
        int received = coupon.getReceiveCount() != null ? coupon.getReceiveCount() : 0;
        if (total > 0 && received >= total) {
            throw new RRException("优惠券已被领完");
        }
        // 每人限领
        int perLimit = coupon.getPerLimit() != null && coupon.getPerLimit() > 0
                ? coupon.getPerLimit() : 1;
        long already = couponHistoryService.count(new LambdaQueryWrapper<CouponHistoryEntity>()
                .eq(CouponHistoryEntity::getCouponId, couponId)
                .eq(CouponHistoryEntity::getMemberId, memberId));
        if (already >= perLimit) {
            throw new RRException("已达每人限领数量");
        }
        // 写领取记录
        CouponHistoryEntity history = new CouponHistoryEntity();
        history.setCouponId(couponId);
        history.setMemberId(memberId);
        history.setGetType(1); // 主动领取
        history.setUseType(0); // 未使用
        history.setCreateTime(now);
        couponHistoryService.save(history);
        // 领取数 +1（原子自增）
        this.update(new LambdaUpdateWrapper<CouponEntity>()
                .eq(CouponEntity::getId, couponId)
                .setSql("receive_count = ifnull(receive_count, 0) + 1"));
    }

    @Override
    public void updatePublish(Long id, Integer publish) {
        this.update(new LambdaUpdateWrapper<CouponEntity>()
                .eq(CouponEntity::getId, id)
                .set(CouponEntity::getPublish, publish));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyCoupon(Long id) {
        CouponEntity source = this.getById(id);
        if (source == null) {
            throw new RRException("优惠券不存在");
        }
        CouponEntity copy = new CouponEntity();
        BeanUtils.copyProperties(source, copy);
        copy.setId(null);
        copy.setReceiveCount(0);
        copy.setUseCount(0);
        copy.setPublish(0); // 复制后默认未发布，管理员确认后再发布
        copy.setCouponName(source.getCouponName() + "(副本)");
        this.save(copy);
        return copy.getId();
    }

}