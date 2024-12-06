package com.rickey.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.rickey.backend.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.rickey.backend.model.enums.InterfaceInfoStatusEnum;
import com.rickey.backend.service.InterfaceInfoService;
import com.rickey.backend.service.UserInterfaceInfoService;
import com.rickey.backend.service.UserService;
import com.rickey.backend.utils.RedisUtil;
import com.rickey.common.annotation.AuthCheck;
import com.rickey.common.common.BaseResponse;
import com.rickey.common.common.DeleteRequest;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.common.IdRequest;
import com.rickey.common.constant.CommonConstant;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.entity.InterfaceInfo;
import com.rickey.common.model.entity.UserInterfaceInfo;
import com.rickey.common.utils.ResultUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 接口管理
 */
@RestController
@RequestMapping("/interfaceInfo")
@RequiredArgsConstructor
@Slf4j
public class InterfaceInfoController {

    static final String BASE_URL = "http://localhost:8123/api";

    private static final String CACHE_KEY_PREFIX_INTERFACE = "interfaceInfo:";

    private static final String CACHE_KEY_PREFIX_INTERFACE_PAGE = "interfaceInfoPage:";

    private static final String CACHE_KEY_PREFIX_INTERFACE_ADMIN = "interfaceInfoListGetByAdmin";

    private final static String COOKIE_NAME = "rickey_login_token";

    private static final String GATEWAY_HOST = "http://localhost:8090";

    private final InterfaceInfoService interfaceInfoService;

    private final UserService userService;

    private final RedisTemplate redisTemplate;

    private final RedisUtil redisUtil;

    private final UserInterfaceInfoService userInterfaceInfoService;

    private static final Gson gson = new Gson();

    @Autowired
    public InterfaceInfoController(InterfaceInfoService interfaceInfoService, UserInterfaceInfoService userInterfaceInfoService, RedisUtil redisUtil, RedisTemplate redisTemplate, UserService userService) {
        this.interfaceInfoService = interfaceInfoService;
        this.userInterfaceInfoService = userInterfaceInfoService;
        this.redisUtil = redisUtil;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
    }

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
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<InterfaceInfo>> listInterfaceInfoByPage(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = interfaceInfoQueryRequest.getCurrent();
        long size = interfaceInfoQueryRequest.getPageSize();
        log.info("currentPage = {}", current);
        log.info("pageSize = {}", size);
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
        log.info("接口数据 = {}", interfaceInfoPage);

        // 将数据放入缓存
        redisUtil.set(cacheKey, interfaceInfoPage, 300);

        return ResultUtils.success(interfaceInfoPage);
    }

    /**
     * 发布接口
     *
     * @param idRequest
     * @param request
     * @return
     */
    @PostMapping("/online")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> onlineInterfaceInfo(@RequestBody IdRequest idRequest,
                                                     HttpServletRequest request) {
        //1.判断接口是否存在
        //2.判断接口是否可以调用
        //3.修改接口的状态为发布状态
        if (idRequest == null || idRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        InterfaceInfo interfaceInfo = interfaceInfoService.getById(id);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        String accessKey = request.getHeader("accessKey");
        String secretKey = request.getHeader("secretKey");

        Object res = invokeInterfaceInfo(interfaceInfo.getSdk(), interfaceInfo.getName(), interfaceInfo.getRequestParams(), accessKey, secretKey);
        if (res == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (res.toString().contains("Error request")) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统接口内部异常");
        }

        InterfaceInfo updateInterfaceInfo = new InterfaceInfo();
        updateInterfaceInfo.setId(id);
        updateInterfaceInfo.setStatus(InterfaceInfoStatusEnum.ONLINE.getValue());
        boolean result = interfaceInfoService.updateById(updateInterfaceInfo);
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
     *  在线调用接口
     * @param interfaceInfoInvokeRequest
     * @param request
     * @return
     */
    @PostMapping("/invoke")
    public BaseResponse<Object> invokeInterfaceInfo(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
                                                    HttpServletRequest request) throws UnsupportedEncodingException {
        // 1.判断接口是否存在
        if (interfaceInfoInvokeRequest == null || interfaceInfoInvokeRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // request.setCharacterEncoding("utf-8");
        log.info("params = {}", interfaceInfoInvokeRequest.getUserRequestParams());
        long id = interfaceInfoInvokeRequest.getId();
        InterfaceInfo interfaceInfo = interfaceInfoService.getById(id);
        log.info("调用的接口id = {}", id);
        if (interfaceInfo == null) {
            log.error("请求参数为空，无法处理接口调用，接口信息：{}", interfaceInfoInvokeRequest);
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String accessKey = request.getHeader("accessKey");
        String secretKey = request.getHeader("secretKey");
        String userId = request.getHeader("userId");

        // 2.用户调用次数校验
        QueryWrapper<UserInterfaceInfo> userInterfaceInfoQueryWrapper = new QueryWrapper<>();
        userInterfaceInfoQueryWrapper.eq("userId", userId);
        userInterfaceInfoQueryWrapper.eq("interfaceInfoId", id);
        UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.getOne(userInterfaceInfoQueryWrapper);
        if (userInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用次数不足！");
        }
        int leftNum = userInterfaceInfo.getLeftNum();
        if (leftNum <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用次数不足！");
        }
        // 3.发起接口调用
        String requestParams = interfaceInfoInvokeRequest.getUserRequestParams();
        log.info("sdk = {}", interfaceInfo.getSdk());
        log.info("name = {}", interfaceInfo.getName());
        log.info("requestParams = {}", requestParams);
        Object res = invokeInterfaceInfo(interfaceInfo.getSdk(), interfaceInfo.getName(), requestParams, accessKey, secretKey);
        if (res == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (res.toString().contains("Error request")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用错误，请检查参数和接口调用次数！");
        }
        return ResultUtils.success(res);
    }

    private Object invokeInterfaceInfo(String classPath, String methodName, String userRequestParams,
                                       String accessKey, String secretKey) {
        try {
            // 1. 动态加载目标类（根据类的路径classPath）
            log.info("加载类：{}", classPath);
            Class<?> clientClazz = Class.forName(classPath);

            // 2. 获取类的构造器，构造器需要两个参数：accessKey和secretKey
            log.info("获取构造器，构造器参数：accessKey, secretKey");
            Constructor<?> binApiClientConstructor = clientClazz.getConstructor(String.class, String.class);

            // 3. 通过构造器创建客户端实例
            log.info("通过构造器创建API客户端实例...");
            Object apiClient = binApiClientConstructor.newInstance(accessKey, secretKey);

            // 4. 查找目标方法（methodName）
            log.info("查找目标方法：{}", methodName);
            Method[] methods = clientClazz.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    // 4.1 如果方法的参数列表为空，直接调用该方法
                    log.info("找到目标方法：{}", methodName);
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == 0) {
                        log.info("方法无参数，直接调用...");
                        return method.invoke(apiClient);
                    }

                    // 4.2 如果方法需要参数，构造参数
                    log.info("方法需要参数，解析请求参数...");
                    Object parameter = gson.fromJson(userRequestParams, parameterTypes[0]);
                    log.info("解析后的参数：{}", parameter);

                    // 4.3 调用该方法并传入构造的参数
                    log.info("调用方法：{}，并传入参数", methodName);
                    return method.invoke(apiClient, parameter);
                }
            }

            // 如果没有找到对应的方法，返回null
            log.warn("未找到对应的目标方法：{}", methodName);
            return null;
        } catch (Exception e) {
            // 捕获异常并打印堆栈信息，抛出业务异常
            log.error("调用接口发生异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "找不到调用的方法!! 请检查你的请求参数是否正确!");
        }
    }


}
