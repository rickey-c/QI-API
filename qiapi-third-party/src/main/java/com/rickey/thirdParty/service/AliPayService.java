package com.rickey.thirdParty.service;

import com.alipay.api.AlipayApiException;
import com.rickey.thirdParty.model.entity.AliPayInfo;

/**
 * @Auther: csp1999
 * @Date: 2020/11/13/21:55
 * @Description: 支付 service
 */
public interface AliPayService {
    /**
     * 支付宝支付接口
     *
     * @param aliPayInfo
     * @return
     * @throws AlipayApiException
     */
    String aliPay(AliPayInfo aliPayInfo) throws AlipayApiException;
}
