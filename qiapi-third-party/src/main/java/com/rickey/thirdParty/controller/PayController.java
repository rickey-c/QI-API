package com.rickey.thirdParty.controller;

import com.alipay.api.AlipayApiException;
import com.rickey.thirdParty.model.entity.AliPayInfo;
import com.rickey.thirdParty.service.AliPayService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Auther: rickey
 * @Date: 2020/11/13/21:47
 * @Description: 支付宝沙箱测试 controller
 */
@RestController
public class PayController {
    @Resource
    private AliPayService aliPayService;

    /**
     * 支付宝支付 api
     *
     * @param outTradeNo
     * @param subject
     * @param totalAmount
     * @param description
     * @return
     * @throws AlipayApiException
     */
    @PostMapping(value = "/alipay")
    public String alipay(String outTradeNo, String subject,
                         String totalAmount, String description) throws AlipayApiException {
        AliPayInfo aliPayInfo = new AliPayInfo();
        aliPayInfo.setOut_trade_no(outTradeNo);
        aliPayInfo.setSubject(subject);
        aliPayInfo.setTotal_amount(totalAmount);
        aliPayInfo.setDescription(description);
        System.out.println(aliPayInfo);
        return aliPayService.aliPay(aliPayInfo);
    }
}
