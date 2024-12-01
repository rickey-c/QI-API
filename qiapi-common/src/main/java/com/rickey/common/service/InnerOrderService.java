package com.rickey.common.service;

import com.rickey.common.model.entity.Order;

/**
 * 内部订单信息服务
 */

public interface InnerOrderService {
    Order getOrderById(Long id);
}
