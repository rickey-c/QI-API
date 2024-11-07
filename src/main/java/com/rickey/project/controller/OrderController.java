package com.rickey.project.controller;

import com.rickey.project.common.BaseResponse;
import com.rickey.project.common.ErrorCode;
import com.rickey.project.common.ResultUtils;
import com.rickey.project.exception.BusinessException;
import com.rickey.project.model.dto.order.OrderAddRequest;
import com.rickey.project.service.OrderService;
import com.rickey.project.service.UserService;
import com.rickey.clientSDK.client.PayClient;
import com.rickey.common.model.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    static final String BASE_URL = "http://localhost:8123/api";

    @Resource
    private OrderService orderService;

    @Resource
    private UserService userService;

    @Resource
    private PayClient payClient;

    /**
     * 增删查改
     */


    /**
     * 创建
     *
     * @param orderAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addOrder(@RequestBody OrderAddRequest orderAddRequest, HttpServletRequest request) {
        if (orderAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Order order = new Order();
        BeanUtils.copyProperties(orderAddRequest, order);
        // 校验
        orderService.validOrder(order, true);
        boolean result = orderService.save(order);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newOrderId = order.getId();
        return ResultUtils.success(newOrderId);
    }

}
