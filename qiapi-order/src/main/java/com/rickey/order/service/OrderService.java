package com.rickey.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rickey.common.model.entity.Order;

public interface OrderService extends IService<Order> {
    void validOrder(Order order, boolean b);

    boolean updateOrderStatus(Long orderId);

    Order getOrderById(Long id);
}
