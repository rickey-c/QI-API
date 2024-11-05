package com.rickey.project.controller;

import com.rickey.project.annotation.AuthCheck;
import com.rickey.project.common.BaseResponse;
import com.rickey.project.common.ErrorCode;
import com.rickey.project.common.ResultUtils;
import com.rickey.project.exception.BusinessException;
import com.rickey.project.mapper.InterfaceInfoMapper;
import com.rickey.project.mapper.UserInterfaceInfoMapper;
import com.rickey.project.service.InterfaceInfoService;
import com.rickey.qiapicommon.model.entity.InterfaceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分析控制器
 */
@RestController
@RequestMapping("/analysis")
@Slf4j
public class AnalysisController {

    @Resource
    private UserInterfaceInfoMapper userInterfaceInfoMapper;

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Resource
    private InterfaceInfoMapper interfaceInfoMapper;

    /**
     * 获取调用次数最多的接口信息
     *
     * @return 调用次数最多的接口信息列表
     */
    @GetMapping("/top/interface/invoke")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<List<InterfaceInfo>> listTopInvokeInterfaceInfo() {
        // 根据interface表直接查询调用次数最多的前3个接口信息
        List<InterfaceInfo> interfaceInfoList = interfaceInfoMapper.listTopInvokeInterfaceInfo(3);

        // 如果查询结果为空，抛出系统错误异常
        if (CollectionUtils.isEmpty(interfaceInfoList)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        // 返回结果
        return ResultUtils.success(interfaceInfoList);
    }
}