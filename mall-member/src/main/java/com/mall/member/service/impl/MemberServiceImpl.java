package com.mall.member.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mall.common.utils.HttpUtils;
import com.mall.member.dao.MemberLevelDao;
import com.mall.member.entity.MemberLevelEntity;
import com.mall.member.exception.PhoneException;
import com.mall.member.exception.UsernameException;
import com.mall.member.vo.MemberUserLoginVo;
import com.mall.member.vo.MemberUserRegisterVo;
import com.mall.member.vo.SocialUser;
import com.mall.member.vo.WeChatUserVo;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.member.dao.MemberDao;
import com.mall.member.entity.MemberEntity;
import com.mall.member.service.MemberService;
import lombok.RequiredArgsConstructor;


@Service("memberService")
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    private final MemberLevelDao memberLevelDao;

    // 微博 API 基址（来自 Nacos mall-member 配置）
    @Value("${weibo.api-host:https://api.weibo.com}")
    private String weiboApiHost;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String) params.get("key");
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new LambdaQueryWrapper<MemberEntity>()
        .and(StringUtils.isNotBlank(key), w -> w
                .like(MemberEntity::getMobile, key)
                .or().like(MemberEntity::getNickname, key))
        );

        return new PageUtils(page);
    }

    @Override
    public void register(MemberUserRegisterVo vo) {
        MemberEntity memberEntity = new MemberEntity();

        // 设置默认等级（等级表可能为空，判空避免 NPE）
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        if (levelEntity != null) {
            memberEntity.setLevelId(levelEntity.getId());
        }

        // 设置其它的默认信息
        // 检查用户名和手机号是否唯一。感知异常，异常机制
        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());

        memberEntity.setNickname(vo.getUserName());
        memberEntity.setUsername(vo.getUserName());
        // 密码进行MD5加密
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String encode = bCryptPasswordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);
        memberEntity.setMobile(vo.getPhone());
        memberEntity.setGender(0);
        memberEntity.setCreateTime(new Date());

        // 保存数据
        this.baseMapper.insert(memberEntity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneException {

        Long phoneCount = this.baseMapper.selectCount(new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getMobile, phone));

        if (phoneCount > 0) {
            throw new PhoneException();
        }

    }

    @Override
    public void checkUserNameUnique(String userName) throws UsernameException {

        Long usernameCount = this.baseMapper.selectCount(new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getUsername, userName));

        if (usernameCount > 0) {
            throw new UsernameException();
        }
    }

    @Override
    public MemberEntity login(MemberUserLoginVo vo) {
        String loginUser = vo.getLoginUser();
        String password = vo.getPassword();

        //1、去数据库查询 SELECT * FROM ums_member WHERE username = ? OR mobile = ?
        MemberEntity memberEntity = this.baseMapper.selectOne(new LambdaQueryWrapper<MemberEntity>()
                .eq(MemberEntity::getUsername, loginUser).or().eq(MemberEntity::getMobile, loginUser));

        if (memberEntity == null) {
            //登录失败
            return null;
        } else {
            //获取到数据库里的password
            String password1 = memberEntity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            //进行密码匹配
            boolean matches = passwordEncoder.matches(password, password1);
            if (matches) {
                //登录成功
                return memberEntity;
            }
        }

        return null;
    }

    /**
     * 微博登录
     */
    @Override
    public MemberEntity login(SocialUser socialUser) throws Exception {
        // 具有登录和注册逻辑
        String uid = socialUser.getUid();
        // 1、判断当前社交用户是否已经登录过系统
        MemberEntity memberEntity = this.baseMapper.selectOne(new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getSocialUid, uid));
        if (memberEntity != null) {
            // 这个用户已经注册过
            // 更新访问令牌和有效期，并刷新微博昵称/头像/性别（微博资料可能变化）
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setAccessToken(socialUser.getAccess_token());
            update.setExpiresIn(socialUser.getExpires_in());
            try {
                Map<String, String> query = new HashMap<>();
                query.put("access_token", socialUser.getAccess_token());
                query.put("uid", uid);
                HttpResponse response = HttpUtils.doGet(weiboApiHost, "/2/users/show.json", "get", new HashMap<>(), query);
                if (response.getStatusLine().getStatusCode() == 200) {
                    JSONObject jo = JSON.parseObject(org.apache.http.util.EntityUtils.toString(response.getEntity()));
                    update.setNickname(jo.getString("name"));
                    String gender = jo.getString("gender");
                    update.setGender("m".equals(gender) ? 1 : 0);
                    update.setHeader(jo.getString("profile_image_url"));
                }
            } catch (Exception e) {
                log.warn("微博登录刷新用户资料失败: " + e.getMessage());
            }
            this.baseMapper.updateById(update);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;
        } else {
            // 2、没有查到当前社交用户对应的记录我们就需要注册一个
            MemberEntity register = new MemberEntity();
            // 3、查询当前社交用户的社交账号信息（昵称、性别等）
            Map<String,String> query = new HashMap<>();
            query.put("access_token",socialUser.getAccess_token());
            query.put("uid",socialUser.getUid());
            HttpResponse response = HttpUtils.doGet(weiboApiHost, "/2/users/show.json", "get", new HashMap<>(), query);

            if (response.getStatusLine().getStatusCode() == 200) {
                // 查询成功
                String json = org.apache.http.util.EntityUtils.toString(response.getEntity());
                JSONObject jsonObject = JSON.parseObject(json);
                String name = jsonObject.getString("name");
                String gender = jsonObject.getString("gender");
                String profileImageUrl = jsonObject.getString("profile_image_url");

                register.setNickname(name);
                register.setGender("m".equals(gender)?1:0);
                register.setHeader(profileImageUrl);
                register.setCreateTime(new Date());
                register.setSocialUid(socialUser.getUid());
                register.setAccessToken(socialUser.getAccess_token());
                register.setExpiresIn(socialUser.getExpires_in());

                // 把用户信息插入到数据库中
                this.baseMapper.insert(register);

            }
            return register;
        }
    }

    /**
     * 微信网页授权登录：按 openid(social_uid) 查找，不存在则自动注册
     */
    @Override
    public MemberEntity login(WeChatUserVo weChatUser) {
        String openid = weChatUser.getOpenid();
        if (StringUtils.isBlank(openid)) {
            return null;
        }
        MemberEntity memberEntity = this.baseMapper.selectOne(
                new LambdaQueryWrapper<MemberEntity>().eq(MemberEntity::getSocialUid, openid));
        if (memberEntity != null) {
            // 已注册过：刷新昵称/头像（微信资料可能更新）
            MemberEntity update = new MemberEntity();
            update.setId(memberEntity.getId());
            update.setNickname(weChatUser.getNickname());
            update.setHeader(weChatUser.getHeader());
            if (weChatUser.getGender() != null) {
                update.setGender(weChatUser.getGender());
            }
            this.baseMapper.updateById(update);
            memberEntity.setNickname(weChatUser.getNickname());
            memberEntity.setHeader(weChatUser.getHeader());
            return memberEntity;
        }
        // 首次微信登录：自动注册会员
        MemberEntity register = new MemberEntity();
        register.setNickname(weChatUser.getNickname());
        register.setHeader(weChatUser.getHeader());
        register.setGender(weChatUser.getGender() != null ? weChatUser.getGender() : 0);
        register.setSocialUid(openid);
        register.setStatus(1);
        register.setSourceType(2); // 2-微信
        register.setCreateTime(new Date());
        this.baseMapper.insert(register);
        return register;
    }
}