package com.rickey.project.model.dto.order;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderAddRequest implements Serializable {
    /**
     * 创建订单的用户
     */
    private Long userId;

    /**
     * 用户要购买的接口
     */
    private Long interfaceId;

    /**
     * 购买的调用次数
     */
    private Integer quantity;

    /**
     * 总价格
     */
    private BigDecimal totalPrice;


    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 请求头
     */
    private String requestHeader;

    /**
     * 响应头
     */
    private String responseHeader;

    /**
     * 请求类型
     */
    private String method;
}
