package com.rickey.order.model.dto.order;

import java.math.BigDecimal;

/**
 * @Author: Rickey
 * @CreateTime: 2024-11-19
 * @Description: 支付订单类
 */
public class OrderPayRequest {
    /**
     * 主键id
     */
    private Long id;

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
     * 订单状态
     */
    private Integer status;
}