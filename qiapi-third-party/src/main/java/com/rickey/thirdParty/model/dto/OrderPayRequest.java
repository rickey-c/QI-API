package com.rickey.thirdParty.model.dto;

import lombok.Data;

/**
 * @Author: Rickey
 * @CreateTime: 2024-11-26
 * @Description: 封装支付请求
 */
@Data
public class OrderPayRequest {
    private String outTradeNo;
    private String subject;
    private String totalAmount;
    private String description;
}