package com.mall.seckill.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mall.common.to.mq.SeckillOrderTo;
import com.mall.common.exception.RRException;
import com.mall.common.utils.Result;
import com.mall.common.vo.MemberResponseVo;
import com.mall.seckill.feign.CouponFeignService;
import com.mall.seckill.feign.ProductFeignService;
import com.mall.seckill.interceptor.LoginUserInterceptor;
import com.mall.seckill.service.SeckillService;
import com.mall.seckill.to.SeckillSkuRedisTo;
import com.mall.seckill.vo.SeckillSessionWithSkusVo;
import com.mall.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final CouponFeignService couponFeignService;


    private final ProductFeignService productFeignService;


    private final StringRedisTemplate redisTemplate;


    private final RabbitTemplate rabbitTemplate;

    private final String SESSION__CACHE_PREFIX = "seckill:sessions:";

    private final String SECKILL_CHARE_PREFIX = "seckill:skus";

    // +商品随机码
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    /**
     * Lua 原子扣减库存脚本：
     * 返回 -1=库存未初始化；-2=库存不足；其余=扣减后剩余库存
     */
    private static final DefaultRedisScript<Long> SECKILL_DECR_SCRIPT = new DefaultRedisScript<>(
            "local c = tonumber(redis.call('get', KEYS[1])) " +
                    "if c == nil then return -1 end " +
                    "if c < tonumber(ARGV[1]) then return -2 end " +
                    "redis.call('decrby', KEYS[1], ARGV[1]) " +
                    "return c - tonumber(ARGV[1])",
            Long.class);

    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 1、扫描最近三天的商品需要参加秒杀的活动
        Result<Object> late3DaySession = couponFeignService.getLates3DaySession();
        log.info("获取最近三天秒杀场次: code={}, data={}", late3DaySession.getCode(),
                late3DaySession.getData() == null ? null : JSON.toJSONString(late3DaySession.getData()));
        if (late3DaySession.getCode() == 200 && late3DaySession.getData() != null) {
            // 上架商品（data 直接是 List<SeckillSessionWithSkusVo>，fastjson 反序列化）
            List<SeckillSessionWithSkusVo> sessionData = JSON.parseObject(
                    JSON.toJSONString(late3DaySession.getData()),
                    new TypeReference<List<SeckillSessionWithSkusVo>>() {
                    });
            if (sessionData == null || sessionData.isEmpty()) {
                log.warn("最近三天没有秒杀场次，跳过上架");
                return;
            }
            // 缓存到Redis
            // 1、缓存活动信息
            saveSessionInfos(sessionData);
            // 2、缓存活动的关联商品信息
            saveSessionSkuInfo(sessionData);
            log.info("秒杀商品上架完成: 场次数={}", sessionData.size());
        } else {
            log.warn("获取秒杀场次失败或数据为空: code={}, msg={}", late3DaySession.getCode(),
                    late3DaySession.getMessage());
        }
    }

    /**
     * 获取到当前可以参加秒杀商品的信息
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        // 1、确定当前展示哪个场次：在场场次优先（多个同时段取开始时间最新的一场）；
        //    无在场场次时取「下一个即将开始」的场次（预约秒杀），避免 keys() 顺序随机导致刷新漂移
        long time = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSION__CACHE_PREFIX + "*");
        String currentKey = null;
        long latestStart = -1;
        String upcomingKey = null;
        long earliestStart = Long.MAX_VALUE;
        if (keys != null) {
            for (String key : keys) {
                String[] s = key.replace(SESSION__CACHE_PREFIX, "").split("_");
                if (s.length != 2) {
                    continue;
                }
                try {
                    long startTime = Long.parseLong(s[0]);
                    long endTime = Long.parseLong(s[1]);
                    if (time >= startTime && time <= endTime && startTime > latestStart) {
                        latestStart = startTime;
                        currentKey = key;
                    } else if (startTime > time && startTime < earliestStart) {
                        earliestStart = startTime;
                        upcomingKey = key;
                    }
                } catch (NumberFormatException ignore) {
                    // 忽略异常格式的 key
                }
            }
        }
        if (currentKey == null && upcomingKey != null) {
            currentKey = upcomingKey;
            log.info("当前无在场场次，展示下一场预约秒杀: key={}", upcomingKey);
        }
        if (currentKey != null) {
            log.info("当前秒杀场次: key={}", currentKey);
            return getSessionSkus(currentKey);
        }
        return null;
    }

    /**
     * 分场次秒杀商品：
     * <ul>
     *   <li>live：正在秒杀——在场场次（开始时间最新）中仍可抢购的商品</li>
     *   <li>upcoming：预约秒杀——下一场（开始时间最早 &gt; 当前）的商品</li>
     *   <li>history：历史秒杀——已结束场次的全部商品 + 在场/下一场中售罄、下架的商品</li>
     * </ul>
     */
    @Override
    public Map<String, List<SeckillSkuRedisTo>> getSeckillSessions() {
        long time = new Date().getTime();
        Map<String, List<SeckillSkuRedisTo>> result = new HashMap<>();
        result.put("live", new ArrayList<>());
        result.put("upcoming", new ArrayList<>());
        result.put("history", new ArrayList<>());
        Set<String> keys = redisTemplate.keys(SESSION__CACHE_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return result;
        }
        // 解析场次：key -> [key, start, end]
        List<String[]> sessions = new ArrayList<>();
        for (String key : keys) {
            String[] s = key.replace(SESSION__CACHE_PREFIX, "").split("_");
            if (s.length != 2) {
                continue;
            }
            try {
                Long.parseLong(s[0]);
                Long.parseLong(s[1]);
                sessions.add(new String[]{key, s[0], s[1]});
            } catch (NumberFormatException ignore) {
                // 忽略异常格式的 key
            }
        }
        // 在场场次（多个取开始最新）；下一场（开始最早）；历史场次（已结束的全部）
        String liveKey = null;
        long liveStart = -1;
        String upcomingKey = null;
        long upStart = Long.MAX_VALUE;
        List<String> historyKeys = new ArrayList<>();
        for (String[] e : sessions) {
            long start = Long.parseLong(e[1]);
            long end = Long.parseLong(e[2]);
            if (time >= start && time <= end) {
                if (start > liveStart) {
                    liveStart = start;
                    liveKey = e[0];
                }
            } else if (start > time && start < upStart) {
                upStart = start;
                upcomingKey = e[0];
            } else if (end < time) {
                historyKeys.add(e[0]);
            }
        }
        // 拉取商品并归类
        List<SeckillSkuRedisTo> history = result.get("history");
        if (liveKey != null) {
            for (SeckillSkuRedisTo sku : getSessionSkus(liveKey)) {
                if (isPurchasable(sku)) {
                    result.get("live").add(sku);
                } else {
                    history.add(sku); // 售罄/下架归入历史
                }
            }
        }
        if (upcomingKey != null) {
            for (SeckillSkuRedisTo sku : getSessionSkus(upcomingKey)) {
                if (isOnShelf(sku)) {
                    result.get("upcoming").add(sku);
                } else {
                    history.add(sku); // 预约场次中已下架的商品归入历史
                }
            }
        }
        for (String hk : historyKeys) {
            history.addAll(getSessionSkus(hk));
        }
        log.info("秒杀分场次: live={}, upcoming={}, history={}",
                result.get("live").size(), result.get("upcoming").size(), history.size());
        return result;
    }

    /**
     * 可抢购：上架且剩余库存 &gt; 0
     */
    private boolean isPurchasable(SeckillSkuRedisTo sku) {
        return isOnShelf(sku) && sku.getStock() != null && sku.getStock() > 0;
    }

    /**
     * 上架中：shelfStatus 为空视为上架（历史缓存兼容）
     */
    private boolean isOnShelf(SeckillSkuRedisTo sku) {
        return sku.getShelfStatus() == null || sku.getShelfStatus() != 0;
    }

    /**
     * 拉取单个场次 key 的商品列表（去重 + 填充库存）
     */
    private List<SeckillSkuRedisTo> getSessionSkus(String key) {
        List<String> range = redisTemplate.opsForList().range(key, 0, -1);
        if (range == null) {
            return Collections.emptyList();
        }
        range = range.stream().distinct().collect(Collectors.toList());
        BoundHashOperations<String, String, String> hasOps = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
        List<String> listValue = hasOps.multiGet(range);
        if (listValue == null) {
            return Collections.emptyList();
        }
        return listValue.stream()
                .filter(Objects::nonNull)
                .map(item -> JSON.parseObject(item, SeckillSkuRedisTo.class))
                .filter(Objects::nonNull)
                .map(this::fillStock)
                .collect(Collectors.toList());
    }

    /**
     * 填充剩余库存：信号量不存在视为 0（已售罄/已下架/未上架）
     */
    private SeckillSkuRedisTo fillStock(SeckillSkuRedisTo redisTo) {
        redisTo.setStock(getStock(redisTo.getRandomCode()));
        return redisTo;
    }

    /**
     * 读取信号量剩余库存；key 不存在或读取异常返回 0
     */
    private int getStock(String randomCode) {
        if (StringUtils.isEmpty(randomCode)) {
            return 0;
        }
        try {
            String stock = redisTemplate.opsForValue().get(SKU_STOCK_SEMAPHORE + randomCode);
            return stock == null ? 0 : Integer.parseInt(stock);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 根据skuId查询商品是否参加秒杀活动
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckilInfo(Long skuId) {
        // 1、找到所有需要秒杀的商品的key信息---seckill:skus
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
        // 拿到所有的key
        Set<String> keys = hashOps.keys();
        if (keys != null && !keys.isEmpty()) {
            // 正则表达式进行匹配
            String reg = "\\d-" + skuId;
            for (String key : keys) {
                // 如果匹配上了
                if (Pattern.matches(reg, key)) {
                    // 从Redis中取出数据来
                    String redisValue = hashOps.get(key);
                    // 进行序列化
                    SeckillSkuRedisTo redisTo = JSON.parseObject(redisValue, SeckillSkuRedisTo.class);
                    // 随机码
                    long currentTime = new Date().getTime();
                    Long startTime = redisTo.getStartTime();
                    Long endTime = redisTo.getEndTime();
                    // 如果当前时间大于等于秒杀活动开始时间并且要小于活动结束时间
                    if (currentTime >= startTime && currentTime <= endTime) {
                        // 填充剩余库存（供秒杀详情页展示/禁用按钮）
                        redisTo.setStock(getStock(redisTo.getRandomCode()));
                        return redisTo;
                    }
                    redisTo.setRandomCode(null);
                    return redisTo;
                }
            }
        }
        return null;
    }

    /**
     * 当前商品进行秒杀（秒杀开始）
     */
    @Override
    public String kill(String killId, String key, Integer num) {
        long s1 = new Date().getTime();
        // 获取当前用户的信息
        MemberResponseVo user = LoginUserInterceptor.loginUser.get();
        if (user == null || user.getId() == null) {
            throw new RRException("请先登录");
        }

        // 1、获取当前秒杀商品的详细信息从Redis中获取
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
        String skuInfoValue = hashOps.get(killId);
        if (StringUtils.isEmpty(skuInfoValue)) {
            throw new RRException("秒杀商品不存在或已下架");
        }
        // 合法性校验
        SeckillSkuRedisTo redisTo = JSON.parseObject(skuInfoValue, SeckillSkuRedisTo.class);
        Long startTime = redisTo.getStartTime();
        Long endTime = redisTo.getEndTime();
        long currentTime = new Date().getTime();
        // 判断当前这个秒杀请求是否在活动时间区间内(校验时间的合法性)
        if (currentTime < startTime || currentTime > endTime) {
            throw new RRException("秒杀未开始或已结束");
        }
        // 2、校验随机码和商品id
        String randomCode = redisTo.getRandomCode();
        String skuId = redisTo.getPromotionSessionId() + "-" + redisTo.getSkuId();
        if (!randomCode.equals(key) || !killId.equals(skuId)) {
            throw new RRException("秒杀链接不合法");
        }
        // 3、简单防刷：同一会员每分钟最多 20 次秒杀请求
        String limitKey = "seckill:kill:limit:" + user.getId();
        Long reqCnt = redisTemplate.opsForValue().increment(limitKey);
        if (reqCnt != null && reqCnt == 1) {
            redisTemplate.expire(limitKey, 60, TimeUnit.SECONDS);
        }
        if (reqCnt != null && reqCnt > 20) {
            throw new RRException("操作太频繁，请稍后再试");
        }

        // 4、验证购物数量是否合理
        Integer seckillLimit = redisTo.getSeckillLimit();
        if (num == null || num <= 0 || num > seckillLimit) {
            throw new RRException("秒杀数量超出限购");
        }

        // 5、限购计数（按每人限购数量）：INCR 已抢数量，未达上限可再次抢购（取消/超时关单会 DECR 释放）
        String redisKey = user.getId() + "-" + skuId;
        long ttl = endTime - currentTime;
        Long bought = redisTemplate.opsForValue().increment(redisKey);
        if (bought != null && bought == 1) {
            redisTemplate.expire(redisKey, ttl, TimeUnit.MILLISECONDS);
        }
        if (bought != null && bought > seckillLimit) {
            // 超过限购：回滚计数，释放名额
            redisTemplate.opsForValue().decrement(redisKey);
            throw new RRException("每人限购 " + seckillLimit + " 件，已达上限");
        }

        // 6、Lua 脚本原子扣减库存（get + 校验 + decrby 一步完成，防超卖）
        Long remain = redisTemplate.execute(
                SECKILL_DECR_SCRIPT,
                Collections.singletonList(SKU_STOCK_SEMAPHORE + randomCode),
                String.valueOf(num));
        if (remain == null || remain < 0) {
            // 扣减失败：回滚限购计数，避免占着名额
            redisTemplate.opsForValue().decrement(redisKey);
            if (remain != null && remain == -1) {
                throw new RRException("秒杀库存未初始化");
            }
            throw new RRException("秒杀商品已抢完");
        }
        // 创建订单号并发送到 MQ 异步建单（淘宝式排队：点击只锁库存+占位，订单由消费端落库）
        String timeId = IdWorker.getTimeId();
        SeckillOrderTo orderTo = new SeckillOrderTo();
        orderTo.setOrderSn(timeId);
        orderTo.setMemberId(user.getId());
        orderTo.setNum(num);
        orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
        orderTo.setSkuId(redisTo.getSkuId());
        orderTo.setSeckillPrice(redisTo.getSeckillPrice());
        rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
        long s2 = new Date().getTime();
        log.info("秒杀排队成功，订单号：{}，耗时 {} ms", timeId, s2 - s1);
        return timeId;
    }

    private void saveSessionSkuInfo(List<SeckillSessionWithSkusVo> sessionData) {
        sessionData.forEach(session -> {
            // 无关联商品的场次跳过，避免 leftPushAll 空列表报错
            if (session.getRelationSkus() == null || session.getRelationSkus().isEmpty()) {
                return;
            }
            // 获取当前活动的开始和结束时间的时间戳
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            // 存入到Redis中的key
            String key = SESSION__CACHE_PREFIX + startTime + "_" + endTime;
            // 获取到活动中所有商品的skuId（killId）
            List<String> skuIds = session.getRelationSkus().stream()
                    .map(item -> item.getPromotionSessionId() + "-" + item.getSkuId()
                            .toString()).collect(Collectors.toList());
            // 判断Redis中是否有该信息
            Boolean hasKey = redisTemplate.hasKey(key);
            if (!hasKey) {
                // 缓存活动信息
                redisTemplate.opsForList().leftPushAll(key, skuIds);
                log.info("缓存秒杀场次: key={}, skuIds={}", key, skuIds);
            } else {
                // 场次已存在：增量补充后加的关联商品，避免新商品不在列表
                List<String> existing = redisTemplate.opsForList().range(key, 0, -1);
                List<String> missing = skuIds.stream()
                        .filter(id -> existing == null || !existing.contains(id))
                        .collect(Collectors.toList());
                if (!missing.isEmpty()) {
                    redisTemplate.opsForList().leftPushAll(key, missing);
                    log.info("增量补充秒杀场次: key={}, 新增={}", key, missing);
                }
                // 已移除关联的商品：保留场次列表条目（数据不删），由 saveSessionInfos 将库存置 0 隐藏
                if (existing != null) {
                    List<String> stale = existing.stream()
                            .filter(id -> !skuIds.contains(id))
                            .collect(Collectors.toList());
                    if (!stale.isEmpty()) {
                        log.info("秒杀场次存在已移除关联商品（库存置0隐藏，不删数据）: key={}, 列表={}", key, stale);
                    }
                }
            }
        });
    }

    private void saveSessionInfos(List<SeckillSessionWithSkusVo> sessionData) {
        sessionData.forEach(session -> {
            // 无关联商品的场次跳过
            if (session.getRelationSkus() == null || session.getRelationSkus().isEmpty()) {
                return;
            }
            // 准备hash操作，绑定hash
            BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
            session.getRelationSkus().forEach(seckillSkuVo -> {
                // 生成随机码
                String token = UUID.randomUUID().toString().replace("-", "");
                String redisKey = seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId().toString();
                if (!operations.hasKey(redisKey)) {
                    // 缓存我们商品信息
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    Long skuId = seckillSkuVo.getSkuId();
                    // 1、先查询sku的基本信息，调用远程服务（Result.success code=200，data 为实体直接解析）
                    try {
                        Result<Object> info = productFeignService.getSkuInfo(skuId);
                        if (info.getCode() == 200 && info.getData() != null) {
                            SkuInfoVo skuInfo = JSON.parseObject(JSON.toJSONString(info.getData()), SkuInfoVo.class);
                            redisTo.setSkuInfo(skuInfo);
                        } else {
                            log.warn("查询 sku {} 信息失败: code={}", skuId, info.getCode());
                        }
                    } catch (Exception e) {
                        // 单个 sku 查询失败不阻塞整个上架
                        log.error("查询 sku {} 信息异常: {}", skuId, e.getMessage());
                    }
                    // 2、sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo, redisTo);
                    // 上架状态持久化到缓存（管理端下架后即使 Redis 重建也保持下架）
                    redisTo.setShelfStatus(seckillSkuVo.getShelfStatus() == null
                            ? 1 : seckillSkuVo.getShelfStatus());
                    // 3、设置当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    // 4、设置商品的随机码（防止恶意攻击）
                    redisTo.setRandomCode(token);

                    // 序列化json格式存入Redis中
                    String seckillValue = JSON.toJSONString(redisTo);
                    operations.put(seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId()
                            .toString(), seckillValue);
                    log.info("缓存秒杀商品: redisKey={}, skuName={}", redisKey,
                            redisTo.getSkuInfo() == null ? "null" : redisTo.getSkuInfo().getSkuName());

                    // 如果当前这个场次的商品库存信息已经上架就不需要上架
                    // 5、库存计数器（普通 String key，配合 Lua 原子扣减）；下架商品库存置 0
                    if (redisTo.getShelfStatus() != null && redisTo.getShelfStatus() == 0) {
                        redisTemplate.opsForValue().set(SKU_STOCK_SEMAPHORE + token, "0");
                    } else {
                        redisTemplate.opsForValue().setIfAbsent(SKU_STOCK_SEMAPHORE + token,
                                String.valueOf(seckillSkuVo.getSeckillCount()));
                    }
                } else {
                    // 缓存已存在：下架收敛——DB 已下架但缓存仍可抢时置 0（防上下架 Redis 同步失败导致商品复活）
                    try {
                        String existing = (String) operations.get(redisKey);
                        if (existing != null) {
                            JSONObject obj = JSON.parseObject(existing);
                            Integer cachedShelf = obj.getInteger("shelfStatus");
                            int dbShelf = seckillSkuVo.getShelfStatus() == null
                                    ? 1 : seckillSkuVo.getShelfStatus();
                            if (dbShelf == 0 && (cachedShelf == null || cachedShelf != 0)) {
                                obj.put("shelfStatus", 0);
                                operations.put(redisKey, obj.toJSONString());
                                String code = obj.getString("randomCode");
                                if (StringUtils.isNotEmpty(code)) {
                                    redisTemplate.opsForValue().set(SKU_STOCK_SEMAPHORE + code, "0");
                                }
                                log.info("上架任务下架收敛: redisKey={} 置为下架、库存置 0", redisKey);
                            }
                        }
                    } catch (Exception e) {
                        log.error("上架任务下架收敛异常: redisKey={}, err={}", redisKey, e.getMessage());
                    }
                }
            });
            // 已移除关联的商品：不删数据，只把库存信号量置 0（列表不再展示、无法再抢购，记录保留）
            List<String> currentKeys = session.getRelationSkus().stream()
                    .map(v -> v.getPromotionSessionId() + "-" + v.getSkuId())
                    .collect(Collectors.toList());
            String sessionPrefix = session.getId() + "-";
            Set<Object> hashKeys = operations.keys();
            if (hashKeys != null) {
                for (Object hk : hashKeys) {
                    String hs = String.valueOf(hk);
                    if (hs.startsWith(sessionPrefix) && !currentKeys.contains(hs)) {
                        try {
                            String val = (String) operations.get(hs);
                            if (val != null) {
                                SeckillSkuRedisTo staleTo = JSON.parseObject(val, SeckillSkuRedisTo.class);
                                if (staleTo != null && StringUtils.isNotEmpty(staleTo.getRandomCode())) {
                                    redisTemplate.opsForValue()
                                            .set(SKU_STOCK_SEMAPHORE + staleTo.getRandomCode(), "0");
                                    log.info("移除关联商品库存置0（数据保留）: redisKey={}", hs);
                                }
                            }
                        } catch (Exception e) {
                            log.error("清理秒杀信号量异常: {}", e.getMessage());
                        }
                    }
                }
            }
        });
    }
}
