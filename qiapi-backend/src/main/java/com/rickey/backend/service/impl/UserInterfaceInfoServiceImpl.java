package com.rickey.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rickey.backend.mapper.UserInterfaceInfoMapper;
import com.rickey.backend.service.UserInterfaceInfoService;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.entity.UserInterfaceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户接口信息服务实现类
 */
@Service
@Slf4j
public class UserInterfaceInfoServiceImpl extends ServiceImpl<UserInterfaceInfoMapper, UserInterfaceInfo>
        implements UserInterfaceInfoService {

    private final UserInterfaceInfoMapper userInterfaceInfoMapper;

    @Autowired
    public UserInterfaceInfoServiceImpl(UserInterfaceInfoMapper userInterfaceInfoMapper) {
        this.userInterfaceInfoMapper = userInterfaceInfoMapper;
    }

    @Override
    public void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add) {
        if (userInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 创建时，所有参数必须非空
        if (add) {
            if (userInterfaceInfo.getInterfaceInfoId() <= 0 || userInterfaceInfo.getUserId() <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口或用户不存在");
            }
        }
        if (userInterfaceInfo.getLeftNum() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "剩余次数不能小于 0");
        }
    }

    @Override
    @Transactional
    public boolean invokeCount(long interfaceInfoId, long userId) {
        // 判断
        if (interfaceInfoId <= 0 || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UpdateWrapper<UserInterfaceInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("interfaceInfoId", interfaceInfoId);
        updateWrapper.eq("userId", userId);
        updateWrapper.gt("leftNum", 0);
        updateWrapper.setSql("leftNum = leftNum - 1, totalNum = totalNum + 1");
        return this.update(updateWrapper);
    }

    // 用来校验是否还有接口调用次数
    @Override
    public boolean isLeftCount(long interfaceInfoId, long userId) {
        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("interfaceInfoId", interfaceInfoId);
        UserInterfaceInfo userInterfaceInfo = this.getOne(queryWrapper);
        return userInterfaceInfo.getLeftNum() > 0;
    }

    /**
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    @Override
    public int getApiRemainingCalls(long interfaceInfoId, long userId) {
        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("interfaceInfoId", interfaceInfoId);
        UserInterfaceInfo userInterfaceInfo = this.getOne(queryWrapper);
        return userInterfaceInfo.getLeftNum();
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
        log.debug("interfaceInfoId={}, userId={}", interfaceInfoId, userId);
        if (interfaceInfoId == 0 || userId <= 0) {
            log.info("interfaceInfoId非法或者userId非法");
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInterfaceInfo userInterfaceInfo = getUserInterfaceInfo(userId, interfaceInfoId);
        log.info("userInterfaceInfo = {}", userInterfaceInfo);
        if (userInterfaceInfo == null) {
            // 创建对应关系
            UserInterfaceInfo newUserInterfaceInfo = new UserInterfaceInfo();
            newUserInterfaceInfo.setUserId(userId);
            newUserInterfaceInfo.setInterfaceInfoId(interfaceInfoId);
            newUserInterfaceInfo.setLeftNum(increment);
            boolean save = save(newUserInterfaceInfo);
            if (!save) {
                log.debug("userInterfaceInfo保存失败");
                return false;
            }
        } else {
            // 直接增加接口调用次数
            boolean updateLeftNumByIncrement = userInterfaceInfoMapper.
                    updateLeftNumByIncrement(userInterfaceInfo.getId(), leftNum, increment);
            if (!updateLeftNumByIncrement) {
                log.debug("接口调用次数更新失败");
            }
        }
        return true;
    }

    /**
     * @param interfaceInfoId
     * @param userId
     * @return
     */
    @Override
    public UserInterfaceInfo getUserInterfaceInfo(long userId, long interfaceInfoId) {
        QueryWrapper<UserInterfaceInfo> userInterfaceInfoQueryWrapper = new QueryWrapper<>();
        userInterfaceInfoQueryWrapper.eq("userId", userId);
        userInterfaceInfoQueryWrapper.eq("interfaceInfoId", interfaceInfoId);
        UserInterfaceInfo userInterfaceInfo = this.getOne(userInterfaceInfoQueryWrapper);
        if (userInterfaceInfo == null) {
            log.info("查不到对应的接口信息");
        }
        return userInterfaceInfo;
    }

}




