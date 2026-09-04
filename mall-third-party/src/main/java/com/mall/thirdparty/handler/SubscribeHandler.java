package com.mall.thirdparty.handler;

import com.mall.thirdparty.adapter.TextBuilder;
import com.mall.thirdparty.service.WxMsgService;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubscribeHandler extends AbstractHandler {

    private final WxMsgService wxMsgService;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context, WxMpService weixinService,
                                    WxSessionManager sessionManager) throws WxErrorException {

        this.logger.info("新关注用户 OPENID: " + wxMessage.getFromUser());

        WxMpXmlOutMessage responseResult = null;
        try {
            responseResult = this.handleSpecial(weixinService, wxMessage);
        } catch (Exception e) {
            this.logger.error(e.getMessage(), e);
        }

        if (responseResult != null) {
            return responseResult;
        }
//        try {
            return TextBuilder.build("感谢关注", wxMessage);
//        } catch (Exception e) {
//            this.logger.error(e.getMessage(), e);
//        }
//        return null;
    }

    /**
     * 处理特殊请求，比如如果是扫码进来的，可以做相应处理
     */
    private WxMpXmlOutMessage handleSpecial(WxMpService weixinService, WxMpXmlMessage wxMessage)
            throws Exception {
        // 未关注用户扫码（subscribe 事件带 qrscene_ 前缀）：走扫码登录流程，推送授权链接
        if (wxMessage.getEventKey() != null && wxMessage.getEventKey().startsWith("qrscene_")) {
            return wxMsgService.scan(weixinService, wxMessage);
        }
        return null;
    }

}
