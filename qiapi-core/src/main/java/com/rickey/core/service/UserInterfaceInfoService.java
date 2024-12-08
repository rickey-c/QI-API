package com.rickey.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rickey.common.model.entity.UserInterfaceInfo;

/**
 * 用户接口信息服务
 */
public interface UserInterfaceInfoService extends IService<UserInterfaceInfo> {

    void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add);

    /**
     * 调用接口统计
     *
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    boolean invokeCount(long interfaceInfoId, long userId);

    boolean isLeftCount(long userId, long interfaceInfoId);

    int getApiRemainingCalls(long interfaceInfoId, long userId);

    boolean updateLeftNum(long interfaceInfoId, long userId, int calls, int increment);

    UserInterfaceInfo getUserInterfaceInfo(long userId, long interfaceInfoId);

}
