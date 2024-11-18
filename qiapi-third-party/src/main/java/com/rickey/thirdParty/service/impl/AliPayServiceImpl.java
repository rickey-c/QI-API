package com.rickey.thirdParty.service.impl;

import com.alipay.api.AlipayApiException;
import com.rickey.thirdParty.component.Alipay;
import com.rickey.thirdParty.model.entity.AliPayInfo;
import com.rickey.thirdParty.service.AliPayService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Auther: csp1999
 * @Date: 2020/11/13/21:56
 * @Description: 支付service 实现类
 */
@Service
public class AliPayServiceImpl implements AliPayService {

    @Resource
    private Alipay alipay;

    @Override
    public String aliPay(AliPayInfo aliPayInfo) throws AlipayApiException {
        return alipay.pay(aliPayInfo);
    }
}
