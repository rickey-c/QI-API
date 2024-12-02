package com.rickey.backend.service.impl.inner;

import com.rickey.backend.service.UserInterfaceInfoService;
import com.rickey.common.model.entity.UserInterfaceInfo;
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

    /**
     * @param interfaceInfoId
     * @param userId
     * @param leftNum
     * @param increment
     * @return
     */
    @Override
    public boolean updateLeftNum(long interfaceInfoId, long userId, int leftNum, int increment) {
        return userInterfaceInfoService.updateLeftNum(interfaceInfoId, userId, leftNum, increment);
    }

    /**
     * @param userId
     * @param interfaceInfoId
     * @return
     */
    @Override
    public UserInterfaceInfo getUserInterfaceInfo(long userId, long interfaceInfoId) {
        return userInterfaceInfoService.getUserInterfaceInfo(userId, interfaceInfoId);
    }


}
