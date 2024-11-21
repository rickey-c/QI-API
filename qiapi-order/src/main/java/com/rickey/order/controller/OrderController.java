package com.rickey.order.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rickey.clientSDK.client.PayClient;
import com.rickey.common.annotation.AuthCheck;
import com.rickey.common.common.BaseResponse;
import com.rickey.common.common.DeleteRequest;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.constant.CommonConstant;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.entity.Order;
import com.rickey.common.utils.ResultUtils;
import com.rickey.order.model.dto.order.OrderAddRequest;
import com.rickey.order.model.dto.order.OrderQueryRequest;
import com.rickey.order.model.dto.order.OrderUpdateRequest;
import com.rickey.order.service.OrderService;
import com.rickey.order.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;


@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    static final String BASE_URL = "http://localhost:8123/api";

    @Resource
    private OrderService orderService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private PayClient payClient;

    private static final String CACHE_KEY_PREFIX_ORDER = "order:";

    private static final String CACHE_KEY_PREFIX_ORDER_PAGE = "orderPage:";
    @Autowired
    private RedisUtil redisUtil;

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
        Long userId = Long.valueOf(request.getHeader("userId"));
        System.out.println("userId = " + userId);
        String userRole = request.getHeader("userRole");
        System.out.println("userRole = " + userRole);
        String accessKey = request.getHeader("accessKey");
        System.out.println("accessKey = " + accessKey);
        String secretKey = request.getHeader("secretKey");
        System.out.println("secretKey = " + secretKey);

        Order order = new Order();
        BeanUtils.copyProperties(orderAddRequest, order);
        // 手动填入当前用户的Id
        order.setUserId(Long.valueOf(userId));

        // 校验
        orderService.validOrder(order, true);
        boolean result = orderService.save(order);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newOrderId = order.getId();
        return ResultUtils.success(newOrderId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteOrder(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = Long.valueOf(request.getHeader("userId"));
        String userRole = request.getHeader("userRole");
        // 判断是否存在
        Order oldOrder = orderService.getById(userId);
        if (oldOrder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldOrder.getUserId().equals(userId) && !userRole.equals("admin")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = orderService.removeById(userId);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param orderUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateOrder(@RequestBody OrderUpdateRequest orderUpdateRequest,
                                             HttpServletRequest request) {
        if (orderUpdateRequest == null || orderUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Order order = new Order();
        BeanUtils.copyProperties(orderUpdateRequest, order);
        // 参数校验
        orderService.validOrder(order, false);
        Long userId = Long.valueOf(request.getHeader("userId"));
        String userRole = request.getHeader("userRole");
        long id = orderUpdateRequest.getId();
        // 判断是否存在
        Order oldOrder = orderService.getById(id);
        if (oldOrder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldOrder.getUserId().equals(userId) && !userRole.equals("admin")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = orderService.updateById(order);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Order> getOrderById(Long id) {
        // 先进行合法性判断
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查缓存
        String cacheKey = CACHE_KEY_PREFIX_ORDER + id;
        Order order = (Order) redisTemplate.opsForValue().get(cacheKey);
        if (order != null) {
            return ResultUtils.success(order); // 返回缓存中的数据
        }
        // 缓存查不到，查数据库
        order = orderService.getById(id);
        // 加入缓存
        redisTemplate.opsForValue().set(cacheKey, order);
        return ResultUtils.success(order);
    }

    /**
     * 管理员查看订单
     *
     * @param orderQueryRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<Order>> listOrder(OrderQueryRequest orderQueryRequest) {
        String cacheKey = "orderList";
        List<Order> orderList = (List<Order>) redisTemplate.opsForValue().get(cacheKey);
        if (orderList != null) {
            return ResultUtils.success(orderList);
        }
        Order orderQuery = new Order();
        if (orderQueryRequest != null) {
            BeanUtils.copyProperties(orderQueryRequest, orderQuery);
        }
        QueryWrapper<Order> queryWrapper = new QueryWrapper<>(orderQuery);
        orderList = orderService.list(queryWrapper);
        redisTemplate.opsForValue().set(cacheKey, orderList);
        return ResultUtils.success(orderList);
    }

    /**
     * 用户分页获取自己的订单
     *
     * @param orderQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Order>> listOrderByPage(OrderQueryRequest orderQueryRequest,
                                                     HttpServletRequest request) {
        if (orderQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = orderQueryRequest.getCurrent();
        long size = orderQueryRequest.getPageSize();
        String sortField = "id";
        String sortOrder = orderQueryRequest.getSortOrder();
        Long userId = Long.valueOf(request.getHeader("userId"));

        // 检查 orderQueryRequest 的字段
        if (orderQueryRequest.getCurrent() == null || orderQueryRequest.getPageSize() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分页参数不能为空");
        }

        // 生成缓存键
        String cacheKey = CACHE_KEY_PREFIX_ORDER_PAGE + current + ":" + size + ":" + sortField + ":" + sortOrder;

        // 尝试从缓存中获取数据
        Page<Order> orderPage = (Page<Order>) redisTemplate.opsForValue().get(cacheKey);
        System.out.println("orderPage from redis = " + orderPage);
        if (orderPage != null) {
            return ResultUtils.success(orderPage); // 返回缓存中的数据
        }

        // description 需支持模糊搜索
        Order orderQuery = new Order();
        BeanUtils.copyProperties(orderQueryRequest, orderQuery);

        // 设置用户ID
        orderQuery.setUserId(userId);

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>(orderQuery);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);

        // 从数据库获取数据
        orderPage = orderService.page(new Page<>(current, size), queryWrapper);
        System.out.println("orderPage from MySQL = " + orderPage);

        // 将数据放入缓存
        boolean set = redisUtil.set(cacheKey, orderPage);
        System.out.println("set = " + set);

        return ResultUtils.success(orderPage);
    }

    /**
     * 支付订单
     *
     * @param orderUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/pay")
    public BaseResponse<Boolean> payOrder(@RequestBody OrderUpdateRequest orderUpdateRequest,
                                          HttpServletRequest request) {
        if (orderUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Order order = new Order();
        BeanUtils.copyProperties(orderUpdateRequest, order);
        // 参数校验
        orderService.validOrder(order, false);
        Long userId = Long.valueOf(request.getHeader("userId"));
        String userRole = request.getHeader("userRole");
        long id = orderUpdateRequest.getId();
        // 判断是否存在
        Order oldOrder = orderService.getById(id);
        if (oldOrder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldOrder.getUserId().equals(userId) && !userRole.equals("admin")) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = orderService.updateById(order);
        return ResultUtils.success(result);
    }

}
