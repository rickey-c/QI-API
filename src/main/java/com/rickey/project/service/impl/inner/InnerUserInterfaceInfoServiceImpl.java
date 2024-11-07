package com.rickey.project.service.impl.inner;

import com.rickey.project.service.UserInterfaceInfoService;
import com.rickey.common.service.InnerUserInterfaceInfoService;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

/**
 * 内部用户接口信息服务实现类
 */
@DubboService
public class InnerUserInterfaceInfoServiceImpl implements InnerUserInterfaceInfoService {

    private static final Logger log = LoggerFactory.getLogger(InnerUserInterfaceInfoServiceImpl.class);
    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

    @Override
    public boolean invokeCount(long interfaceInfoId, long userId) {
        return userInterfaceInfoService.invokeCount(interfaceInfoId, userId);
    }

    @Override
    public boolean hasRemainingInvokeCount(long interfaceInfoId, long userId) {
        return userInterfaceInfoService.isLeftCount(interfaceInfoId, userId);
    }


    @Override
    public int getApiRemainingCalls(long interfaceInfoId, long userId) {
        return userInterfaceInfoService.getApiRemainingCalls(interfaceInfoId, userId);
    }

}
