package com.rickey.backend.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.rickey.backend.annotation.AuthCheck;
import com.rickey.backend.common.*;
import com.rickey.backend.config.SentinelConfig;
import com.rickey.backend.constant.CommonConstant;
import com.rickey.backend.exception.BusinessException;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.rickey.backend.model.enums.InterfaceInfoStatusEnum;
import com.rickey.backend.service.InterfaceInfoService;
import com.rickey.backend.service.UserService;
import com.rickey.clientSDK.client.QiApiClient;
import com.rickey.common.model.entity.InterfaceInfo;
import com.rickey.common.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 接口管理
 */
@RestController
@RequestMapping("/interfaceInfo")
@Slf4j
public class InterfaceInfoController {

    static final String BASE_URL = "http://localhost:8123/api";

    private static final String CACHE_KEY_PREFIX_INTERFACE = "interfaceInfo:";

    private static final String CACHE_KEY_PREFIX_INTERFACE_PAGE = "interfaceInfoPage:";

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Resource
    private UserService userService;

    @Resource
    private QiApiClient qiApiClient;

    @Resource
    private RedisTemplate redisTemplate;

    // region 增删改查

    /**
     * 创建
     *
     * @param interfaceInfoAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addInterfaceInfo(@RequestBody InterfaceInfoAddRequest interfaceInfoAddRequest, HttpServletRequest request) {
        if (interfaceInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoAddRequest, interfaceInfo);
        // 校验
        interfaceInfoService.validInterfaceInfo(interfaceInfo, true);
        User loginUser = userService.getLoginUser(request);
        interfaceInfo.setUserId(loginUser.getId());
        boolean result = interfaceInfoService.save(interfaceInfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newInterfaceInfoId = interfaceInfo.getId();
        return ResultUtils.success(newInterfaceInfoId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteInterfaceInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = interfaceInfoService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param interfaceInfoUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateInterfaceInfo(@RequestBody InterfaceInfoUpdateRequest interfaceInfoUpdateRequest,
                                                     HttpServletRequest request) {
        if (interfaceInfoUpdateRequest == null || interfaceInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoUpdateRequest, interfaceInfo);
        // 参数校验
        interfaceInfoService.validInterfaceInfo(interfaceInfo, false);
        User user = userService.getLoginUser(request);
        long id = interfaceInfoUpdateRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<InterfaceInfo> getInterfaceInfoById(long id) {
        // 先进行合法性判断
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查缓存
        String cacheKey = CACHE_KEY_PREFIX_INTERFACE + id;
        InterfaceInfo interfaceInfo = (InterfaceInfo) redisTemplate.opsForValue().get(cacheKey);
        if (interfaceInfo != null) {
            return ResultUtils.success(interfaceInfo); // 返回缓存中的数据
        }
        // 缓存查不到，查数据库
        interfaceInfo = interfaceInfoService.getById(id);
        log.info(interfaceInfo.toString());
        // 加入缓存
        //redisTemplate.opsForValue().set(cacheKey, interfaceInfo);
        return ResultUtils.success(interfaceInfo);
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param interfaceInfoQueryRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<InterfaceInfo>> listInterfaceInfo(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        String cacheKey = "interfaceInfoList";
        List<InterfaceInfo> interfaceInfoList = (List<InterfaceInfo>) redisTemplate.opsForValue().get(cacheKey);
        if (interfaceInfoList != null) {
            return ResultUtils.success(interfaceInfoList);
        }
        InterfaceInfo interfaceInfoQuery = new InterfaceInfo();
        if (interfaceInfoQueryRequest != null) {
            BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceInfoQuery);
        interfaceInfoList = interfaceInfoService.list(queryWrapper);
        redisTemplate.opsForValue().set(cacheKey, interfaceInfoList);
        return ResultUtils.success(interfaceInfoList);
    }

    /**
     * 分页获取列表
     *
     * @param interfaceInfoQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<InterfaceInfo>> listInterfaceInfoByPage(InterfaceInfoQueryRequest interfaceInfoQueryRequest,
                                                                     HttpServletRequest request) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = interfaceInfoQueryRequest.getCurrent();
        long size = interfaceInfoQueryRequest.getPageSize();
        String sortField = interfaceInfoQueryRequest.getSortField();
        String sortOrder = interfaceInfoQueryRequest.getSortOrder();
        String description = interfaceInfoQueryRequest.getDescription();

        // 生成缓存键
        String cacheKey = CACHE_KEY_PREFIX_INTERFACE_PAGE + current + ":" + size + ":" + description + ":" + sortField + ":" + sortOrder;

        // 尝试从缓存中获取数据
        Page<InterfaceInfo> interfaceInfoPage = (Page<InterfaceInfo>) redisTemplate.opsForValue().get(cacheKey);
        if (interfaceInfoPage != null) {
            return ResultUtils.success(interfaceInfoPage); // 返回缓存中的数据
        }

        // description 需支持模糊搜索
        InterfaceInfo interfaceInfoQuery = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);
        interfaceInfoQuery.setDescription(null); // 清空原描述以避免 SQL 查询中的不必要限制

        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceInfoQuery);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
                CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);

        // 从数据库获取数据
        interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size), queryWrapper);

        // 将数据放入缓存
        redisTemplate.opsForValue().set(cacheKey, interfaceInfoPage);

        return ResultUtils.success(interfaceInfoPage);
    }


    // endregion

    /**
     * 发布
     *
     * @param idRequest
     * @param request
     * @return
     */
    @PostMapping("/online")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> onlineInterfaceInfo(@RequestBody IdRequest idRequest,
                                                     HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 判断该接口是否可以调用
        com.rickey.clientSDK.model.User user = new com.rickey.clientSDK.model.User();
        user.setUsername("test");
        String username = qiApiClient.getUsernameByPost(user);
        if (StringUtils.isBlank(username)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口验证失败");
        }
        // 仅本人或管理员可修改
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceInfoStatusEnum.ONLINE.getValue());
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 下线
     *
     * @param idRequest
     * @param request
     * @return
     */
    @PostMapping("/offline")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> offlineInterfaceInfo(@RequestBody IdRequest idRequest,
                                                      HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceInfoStatusEnum.OFFLINE.getValue());
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * POST方式获取用户名
     *
     * @param interfaceInfoInvokeRequest
     * @param request
     * @return
     */
    @PostMapping("/name/user")
    @SentinelResource(value = "qi-api-interface",
            blockHandler = "blockHandlerPOST", blockHandlerClass = SentinelConfig.class,
            fallback = "fallbackPOST", fallbackClass = SentinelConfig.class)
    public BaseResponse<Object> getUserNameByPost(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
                                                  HttpServletRequest request) {
        // 前端传过来的 id
        if (interfaceInfoInvokeRequest == null || interfaceInfoInvokeRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = interfaceInfoInvokeRequest.getId();
        String userRequestParams = interfaceInfoInvokeRequest.getUserRequestParams();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (oldInterfaceInfo.getStatus() == InterfaceInfoStatusEnum.OFFLINE.getValue()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口已关闭");
        }

        User loginUser = userService.getLoginUser(request);
        String accessKey = loginUser.getAccessKey();
        String secretKey = loginUser.getSecretKey();

        QiApiClient tempClient = new QiApiClient(accessKey, secretKey);
        Gson gson = new Gson();
        com.rickey.clientSDK.model.User user = gson.fromJson(userRequestParams, com.rickey.clientSDK.model.User.class);
        String usernameByPost = tempClient.getUsernameByPost(user);
        return ResultUtils.success(usernameByPost);
    }

    /**
     * 随机毒鸡汤接口
     *
     * @param request
     * @return
     */
    @SentinelResource(value = "qi-api-interface",
            blockHandler = "blockHandlerGET", blockHandlerClass = SentinelConfig.class,
            fallback = "fallbackGET", fallbackClass = SentinelConfig.class)
    @GetMapping("/random/encouragement")
    public BaseResponse<Object> getRandomEncouragement(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String accessKey = loginUser.getAccessKey();
        String secretKey = loginUser.getSecretKey();
        QiApiClient tempClient = new QiApiClient(accessKey, secretKey);
        String randomEncouragement = tempClient.getRandomEncouragement();
        return ResultUtils.success(randomEncouragement);
    }

}
