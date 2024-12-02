package com.rickey.common.service;

import com.rickey.common.model.entity.UserInterfaceInfo;

/**
 * 内部用户接口信息服务
 */
public interface InnerUserInterfaceInfoService {

    /**
     * 调用接口统计
     *
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceInfoId, long userId);

    boolean hasRemainingInvokeCount(long interfaceInfoId, long userId);

    int getApiRemainingCalls(long interfaceInfoId, long userId);

    boolean updateLeftNum(long interfaceInfoId, long userId, int leftNum, int increment);

    UserInterfaceInfo getUserInterfaceInfo(long userId, long interfaceInfoId);
}
