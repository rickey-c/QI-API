package com.rickey.common.service;

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

    boolean addApiRemainingCall(long interfaceInfoId, long userId, int calls);
}
