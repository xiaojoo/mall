package com.mall.seckill.scheduled;

import com.mall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillSkuScheduled {

    private final SeckillService seckillService;


    private final RedissonClient redissonClient;

    // 秒杀商品上架功能的锁
    private final String upload_lock = "seckill:upload:lock";

    // 保证幂等性问题
    @Scheduled(cron = "0 * * * * ? ")
    public void uploadSeckillSkuLatest3Days() {
        // 1、重复上架无需处理
        log.info("上架秒杀的商品...");
        // 分布式锁
        RLock lock = redissonClient.getLock(upload_lock);
        try {
            // 加锁
            lock.lock(10, TimeUnit.SECONDS);
            seckillService.uploadSeckillSkuLatest3Days();
        } catch (Exception e) {
            // 分钟级任务自愈：下游暂不可用（如 coupon 未注册）只 WARN 一行，不刷堆栈
            log.warn("秒杀商品上架失败（下个周期自动重试）: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}
