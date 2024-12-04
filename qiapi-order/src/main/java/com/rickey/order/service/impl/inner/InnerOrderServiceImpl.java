package com.rickey.order.service.impl.inner;

import com.rickey.common.model.entity.Order;
import com.rickey.common.service.InnerOrderService;
import com.rickey.order.service.OrderService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Author: Rickey
 * @CreateTime: 2024-11-27
 * @Description: 内部订单服务实现类
 */
@DubboService
public class InnerOrderServiceImpl implements InnerOrderService {

    @Autowired
    private OrderService orderService;

    /**
     * @param id
     * @return
     */
    @Override
    public Order getOrderById(Long id) {
        return orderService.getOrderById(id);
    }
}