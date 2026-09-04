package com.mall.product.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import java.util.Date;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;
import com.mall.common.utils.Result;
import com.mall.common.exception.RRException;
import com.mall.common.jwt.MemberJwtUtils;

import com.mall.product.dao.SpuCommentDao;
import com.mall.product.entity.SpuCommentEntity;
import com.mall.product.feign.OrderFeignService;
import com.mall.product.service.SpuCommentService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("spuCommentService")
public class SpuCommentServiceImpl extends ServiceImpl<SpuCommentDao, SpuCommentEntity> implements SpuCommentService {

    @Autowired
    private MemberJwtUtils memberJwtUtils;

    @Autowired
    private OrderFeignService orderFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<SpuCommentEntity> wrapper = new LambdaQueryWrapper<>();
        // 支持按 sku / spu 过滤（商品详情页评论 tab 用）
        Object skuId = params.get("skuId");
        if (skuId != null && StringUtils.isNotBlank(String.valueOf(skuId))) {
            wrapper.eq(SpuCommentEntity::getSkuId, skuId);
        }
        Object spuId = params.get("spuId");
        if (spuId != null && StringUtils.isNotBlank(String.valueOf(spuId))) {
            wrapper.eq(SpuCommentEntity::getSpuId, spuId);
        }
        IPage<SpuCommentEntity> page = this.page(
                new Query<SpuCommentEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void saveComment(SpuCommentEntity spuComment, HttpServletRequest request) {
        // 评论归属：优先 JWT 会员 id，缺失时保留前端传入（历史兼容）
        Long memberId = memberJwtUtils.parseMemberId(memberJwtUtils.extractToken(request));
        if (memberId != null) {
            spuComment.setMemberId(memberId);
        }

        // 回复（commentType=1）不受评价条数限制
        if (spuComment.getCommentType() == null || spuComment.getCommentType() != 1) {
            spuComment.setCommentType(0);
            // 仅购买且支付成功的商品可评价（含追加评论）
            checkPaid(memberId, spuComment.getSkuId(), spuComment.getSpuId());
            if (spuComment.getSpuId() != null) {
                LambdaQueryWrapper<SpuCommentEntity> wrapper = new LambdaQueryWrapper<SpuCommentEntity>()
                        .eq(SpuCommentEntity::getSpuId, spuComment.getSpuId())
                        .ne(SpuCommentEntity::getCommentType, 1);
                if (memberId != null) {
                    wrapper.eq(SpuCommentEntity::getMemberId, memberId);
                } else {
                    wrapper.eq(SpuCommentEntity::getMemberNickName, spuComment.getMemberNickName());
                }
                long cnt = this.count(wrapper);
                if (cnt >= 2) {
                    throw new RRException("每个用户最多评价 2 条，已达上限");
                }
                if (cnt == 1) {
                    // 第 2 条默认追加评论：不统计在评分/差评计算中
                    spuComment.setCommentType(2);
                }
            }
        }
        if (spuComment.getCreateTime() == null) {
            spuComment.setCreateTime(new Date());
        }
        this.save(spuComment);
    }

    /**
     * 购买校验：评论（含追加）必须购买且支付成功；未登录或未购买抛 RRException。
     * fail-closed：订单服务不可用时同样拒绝，不放开限制。
     */
    private void checkPaid(Long memberId, Long skuId, Long spuId) {
        if (memberId == null) {
            throw new RRException("请先登录后再评价");
        }
        boolean paid = false;
        try {
            Result<Boolean> r = orderFeignService.paidCheck(memberId, skuId, spuId);
            paid = r != null && Boolean.TRUE.equals(r.getData());
        } catch (Exception e) {
            log.error("购买校验调用异常，拒绝评价: {}", e.getMessage(), e);
        }
        if (!paid) {
            throw new RRException("购买并支付成功后才能评价该商品");
        }
    }

}
