package com.mall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import java.math.BigDecimal;
import com.mall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    // 应用ID,您的APPID，收款账号既是您的APPID对应支付宝账号
    public String app_id;

    // 商户私钥，您的PKCS8格式RSA2私钥
    public String merchant_private_key;

    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    public String alipay_public_key;

    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    public String notify_url;

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    public String return_url;

    // 签名方式
    private String sign_type;

    // 字符编码格式
    private String charset;

    //订单超时时间（支付表单有效期 5 分钟，与延迟关单队列一致）
    private String timeout = "5m";

    // 支付宝网关； https://openapi-sandbox.dl.alipaydev.com/gateway.do（沙箱新地址，2025年8月起生效，旧地址 openapi.alipaydev.com 已失效）
    public String gatewayUrl;

    public String pay(PayVo vo) throws AlipayApiException {

        // 1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        // 2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        // 商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        // 付款金额，必填
        String total_amount = vo.getTotal_amount();
        // 订单名称，必填
        String subject = vo.getSubject();
        // 商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"timeout_express\":\"" + timeout + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应：" + result);
        return result;
    }

    /**
     * 主动查询交易状态（异步通知丢失/延迟时的兜底）
     *
     * @param outTradeNo 商户订单号
     * @return 交易状态：TRADE_SUCCESS / WAIT_BUYER_PAY / TRADE_CLOSED 等；查询异常返回 null
     */
    public String queryTrade(String outTradeNo) throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json", charset, alipay_public_key, sign_type);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\"}");
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        return response.getTradeStatus();
    }

    /**
     * 支付宝退款（订单已关闭但支付成功时自动退款）
     *
     * @param outTradeNo 商户订单号
     * @param amount     退款金额
     * @return 支付宝网关返回码：10000 表示成功
     */
    public String refund(String outTradeNo, BigDecimal amount) throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json", charset, alipay_public_key, sign_type);
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        // out_request_no 固定用订单号：同订单号退款请求幂等，重复通知不会重复退款
        request.setBizContent("{\"out_trade_no\":\"" + outTradeNo
                + "\",\"refund_amount\":\"" + amount.toPlainString()
                + "\",\"out_request_no\":\"" + outTradeNo + "\"}");
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        return response.getCode();
    }
}
