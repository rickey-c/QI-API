package com.rickey.order.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderAddRequest implements Serializable {
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
