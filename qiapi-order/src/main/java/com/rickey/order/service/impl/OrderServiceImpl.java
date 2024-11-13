package com.rickey.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.entity.Order;
import com.rickey.order.mapper.OrderMapper;
import com.rickey.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
        implements OrderService {
    /**
     * @param order
     * @param add
     */
    @Override
    public void validOrder(Order order, boolean add) {
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单信息不能为空");
        }

        Long userId = order.getUserId();
        Long interfaceId = order.getInterfaceId();
        Integer quantity = order.getQuantity();
        BigDecimal totalPrice = order.getTotalPrice();
        log.info("userId " +userId);
        log.info("interfaceId " +interfaceId);
        log.info("quantity " +quantity);
        log.info("totalPrice " +totalPrice);


        // 创建时，所有参数必须非空
        if (add) {
            if (userId == null || interfaceId == null || quantity == null || totalPrice == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "必填字段不能为空");
            }
        }

        // 校验 userId 和 interfaceId 的合法性
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID非法");
        }
        if (interfaceId == null || interfaceId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口ID非法");
        }

        // 校验购买数量
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "购买数量必须大于0");
        }

        // 校验总价格
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "总价格必须大于0");
        }

        // 其他业务规则校验
        if (totalPrice.scale() > 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "总价格最多只能有两位小数");
        }
    }

}
