package com.mall.thirdparty.handler;

import com.mall.thirdparty.service.WxMsgService;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScanHandler extends AbstractHandler {
    private final WxMsgService wxMsgService;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map,
                                    WxMpService wxMpService, WxSessionManager wxSessionManager) {
        // 扫码事件处理
        try {
            return wxMsgService.scan(wxMpService, wxMpXmlMessage);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
