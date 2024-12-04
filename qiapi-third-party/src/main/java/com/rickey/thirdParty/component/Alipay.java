package com.rickey.thirdParty.component;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.rickey.thirdParty.config.AliPayConfig;
import com.rickey.thirdParty.model.entity.AliPayInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Auther: Rickey
 * @Date: 2020/11/13/21:57
 * @Description: 调用支付宝支付的组件
 */
@Component
@Slf4j
public class Alipay {

    private final AliPayConfig alipayConfig;

    @Autowired
    public Alipay(AliPayConfig alipayConfig) {
        this.alipayConfig = alipayConfig;
    }

    /**
     * 支付接口
     *
     * @param aliPayInfo
     * @return
     * @throws AlipayApiException
     */
    public String pay(AliPayInfo aliPayInfo) throws AlipayApiException {

        // 支付宝网关
        String serverUrl = alipayConfig.getGatewayUrl();
        // APPID
        String appId = alipayConfig.getAppId();
        // 商户私钥, 即PKCS8格式RSA2私钥
        String privateKey = alipayConfig.getPrivateKey();
        log.info("商户私钥:{}", privateKey);
        // 格式化为 json 格式
        String format = "JSON";
        // 字符编码格式
        String charset = alipayConfig.getCharset();
        log.info("字符编码格式:{}", charset);
        // 支付宝公钥, 即对应APPID下的支付宝公钥
        String alipayPublicKey = alipayConfig.getPublicKey();
        log.info("支付宝公钥:{}", alipayPublicKey);
        // 签名方式
        String signType = alipayConfig.getSignType();
        log.info("签名方式:{}", signType);
        // 页面跳转同步通知页面路径
        String returnUrl = alipayConfig.getReturnUrl();
        log.info("returnUrl:{}", returnUrl);
        // 服务器异步通知页面路径
        String notifyUrl = alipayConfig.getNotifyUrl();
        log.info("notifyUrl:{}", notifyUrl);

        // 1、获得初始化的AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(
                serverUrl, appId, privateKey, format, charset, alipayPublicKey, signType);

        // 2、设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        // 页面跳转同步通知页面路径
        alipayRequest.setReturnUrl(returnUrl);
        // 服务器异步通知页面路径
        alipayRequest.setNotifyUrl(notifyUrl);
        // 封装参数(以json格式封装)
        alipayRequest.setBizContent(JSON.toJSONString(aliPayInfo));

        // 3、请求支付宝进行付款，并获取支付结果
        String result = alipayClient.pageExecute(alipayRequest).getBody();
        // 返回付款信息
        return result;
    }
}
