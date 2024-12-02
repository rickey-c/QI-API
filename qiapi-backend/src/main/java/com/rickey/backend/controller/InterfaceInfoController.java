package com.rickey.backend.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.rickey.backend.config.SentinelConfig;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.rickey.backend.model.enums.InterfaceInfoStatusEnum;
import com.rickey.backend.service.InterfaceInfoService;
import com.rickey.backend.service.UserService;
import com.rickey.backend.utils.RedisUtil;
import com.rickey.clientSDK.client.QiApiClient;
import com.rickey.common.annotation.AuthCheck;
import com.rickey.common.common.BaseResponse;
import com.rickey.common.common.DeleteRequest;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.common.IdRequest;
import com.rickey.common.constant.CommonConstant;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.entity.InterfaceInfo;
import com.rickey.common.utils.CookieUtil;
import com.rickey.common.utils.ResultUtils;
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

    private static final String CACHE_KEY_PREFIX_INTERFACE_ADMIN = "interfaceInfoListGetByAdmin";

    private final static String COOKIE_NAME = "rickey_login_token";

    private static final String GATEWAY_HOST = "http://localhost:8090";

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Resource
    private UserService userService;

    @Resource
    private QiApiClient qiApiClient;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedisUtil redisUtil;

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
        Long userId = Long.valueOf(request.getHeader("userId"));
        interfaceInfo.setUserId(userId);
        boolean result = interfaceInfoService.save(interfaceInfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newInterfaceInfoId = interfaceInfo.getId();
        // 删除先前的缓存
        String interfaceInfoCache = CACHE_KEY_PREFIX_INTERFACE + interfaceInfo.getId();
        String interfaceInfoPageCache = CACHE_KEY_PREFIX_INTERFACE_PAGE;
        redisUtil.del(interfaceInfoCache, interfaceInfoPageCache, CACHE_KEY_PREFIX_INTERFACE_ADMIN);
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
        Long userId = Long.valueOf(request.getHeader("userId"));
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(userId);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldInterfaceInfo.getUserId().equals(userId) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = interfaceInfoService.removeById(userId);
        // 删除先前的缓存
        String interfaceInfoCache = CACHE_KEY_PREFIX_INTERFACE + oldInterfaceInfo.getId();
        String interfaceInfoPageCache = CACHE_KEY_PREFIX_INTERFACE_PAGE;
        redisUtil.del(interfaceInfoCache, interfaceInfoPageCache, CACHE_KEY_PREFIX_INTERFACE_ADMIN);
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
        Long userId = Long.valueOf(request.getHeader("userId"));
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(userId);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldInterfaceInfo.getUserId().equals(userId) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        // 删除先前的缓存
        String interfaceInfoCache = CACHE_KEY_PREFIX_INTERFACE + interfaceInfo.getId();
        String interfaceInfoPageCache = CACHE_KEY_PREFIX_INTERFACE_PAGE;
        redisUtil.del(interfaceInfoCache, interfaceInfoPageCache, CACHE_KEY_PREFIX_INTERFACE_ADMIN);
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
        redisUtil.set(cacheKey, interfaceInfo, 300);
        return ResultUtils.success(interfaceInfo);
    }

    /**
     * 获取接口列表（仅管理员可使用）
     *
     * @param interfaceInfoQueryRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<InterfaceInfo>> listInterfaceInfo(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        String cacheKey = CACHE_KEY_PREFIX_INTERFACE_ADMIN;
        List<InterfaceInfo> interfaceInfoList = (List<InterfaceInfo>) redisUtil.get(cacheKey);
        if (interfaceInfoList != null) {
            return ResultUtils.success(interfaceInfoList);
        }
        InterfaceInfo interfaceInfoQuery = new InterfaceInfo();
        if (interfaceInfoQueryRequest != null) {
            BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceInfoQuery);
        interfaceInfoList = interfaceInfoService.list(queryWrapper);
        redisUtil.set(cacheKey, interfaceInfoList, 300);
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
        String cacheKey = CACHE_KEY_PREFIX_INTERFACE_PAGE + current;

        // 尝试从缓存中获取数据
        Page<InterfaceInfo> interfaceInfoPage = (Page<InterfaceInfo>) redisUtil.get(cacheKey);
        System.out.println("interfaceInfoPage = " + interfaceInfoPage);
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
        redisUtil.set(cacheKey, interfaceInfoPage, 300);

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
        Long userId = Long.valueOf(request.getHeader("userId"));
        String accessKey = request.getHeader("accessKey");
        String secretKey = request.getHeader("secretKey");

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
        String accessKey = request.getHeader("accessKey");
        System.out.println("accessKey = " + accessKey);
        String secretKey = request.getHeader("secretKey");
        System.out.println("secretKey = " + secretKey);
        String cookieValue = CookieUtil.getCookieValue(request, COOKIE_NAME);
        if (StrUtil.isNotBlank(cookieValue)) {
            // 如果获取到 Token，则调用延长过期时间的方法
            String gatewayResponse = extendSessionExpireTime(cookieValue);
            System.out.println("延长会话结果: " + gatewayResponse);
        }
        // 得到了token，准备调用延长过期时间的方法
        QiApiClient tempClient = new QiApiClient(accessKey, secretKey);
        String randomEncouragement = tempClient.getRandomEncouragement();
        System.out.println("randomEncouragement = " + randomEncouragement);
        System.out.println("接口调用转发");
        return ResultUtils.success(randomEncouragement);
    }

    /**
     * 向网关发送请求延长 Session 过期时间
     *
     * @param token 用户的 Session Token
     * @return 返回网关的响应内容
     */
    public String extendSessionExpireTime(String token) {
        try {
            // 使用 HttpRequest 构造 GET 请求
            HttpResponse httpResponse = HttpRequest.get(GATEWAY_HOST + "/api/interfaceInvoke/extend")
                    .cookie(COOKIE_NAME + "=" + token)
                    .execute();

            // 处理响应
            if (httpResponse.getStatus() == HttpStatus.HTTP_OK) {
                System.out.println("Session 延长成功，状态码：" + httpResponse.getStatus());
            } else {
                System.out.println("Session 延长失败，状态码：" + httpResponse.getStatus());
            }
            return httpResponse.body(); // 返回网关的响应内容
        } catch (Exception e) {
            System.err.println("调用网关接口延长 Session 时间失败：" + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

}
