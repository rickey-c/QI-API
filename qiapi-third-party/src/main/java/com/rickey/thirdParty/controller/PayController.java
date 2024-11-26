package com.rickey.thirdParty.controller;

import com.alipay.api.AlipayApiException;
import com.rickey.common.common.BaseResponse;
import com.rickey.common.utils.ResultUtils;
import com.rickey.thirdParty.model.dto.OrderPayRequest;
import com.rickey.thirdParty.model.entity.AliPayInfo;
import com.rickey.thirdParty.service.AliPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @Auther: rickey
 * @Date: 2020/11/13/21:47
 * @Description: 支付宝沙箱测试 controller
 */
@RestController
@RequestMapping("/alipay")
@Slf4j
public class PayController {
    @Resource
    private AliPayService aliPayService;

    /**
     * 支付宝支付 api
     *
     * @param orderPayRequest
     * @return
     * @throws AlipayApiException
     */
    @PostMapping(value = "/pay")
    public BaseResponse<String> alipay(@RequestBody OrderPayRequest orderPayRequest) throws AlipayApiException {
        AliPayInfo aliPayInfo = new AliPayInfo();
        // 商家订单号
        aliPayInfo.setOut_trade_no(orderPayRequest.getOutTradeNo());
        // 订单标题Q
        aliPayInfo.setSubject(orderPayRequest.getSubject());
        // 总金额
        aliPayInfo.setTotal_amount(orderPayRequest.getTotalAmount());
        // 订单描
        aliPayInfo.setDescription(orderPayRequest.getDescription());
        String form = aliPayService.aliPay(aliPayInfo);
        return ResultUtils.success(form);
    }
}
