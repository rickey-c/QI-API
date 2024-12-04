package com.rickey.thirdParty.service.impl;

import com.alipay.api.AlipayApiException;
import com.rickey.thirdParty.component.Alipay;
import com.rickey.thirdParty.model.entity.AliPayInfo;
import com.rickey.thirdParty.service.AliPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Auther: csp1999
 * @Date: 2020/11/13/21:56
 * @Description: 支付service 实现类
 */
@Service
public class AliPayServiceImpl implements AliPayService {

    private final Alipay alipay;

    @Autowired
    public AliPayServiceImpl(Alipay alipay) {
        this.alipay = alipay;
    }

    @Override
    public String aliPay(AliPayInfo aliPayInfo) throws AlipayApiException {
        return alipay.pay(aliPayInfo);
    }
}
