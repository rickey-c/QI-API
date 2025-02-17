package com.rickey.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rickey.common.model.entity.InterfaceInfo;
import com.rickey.core.mapper.InterfaceInfoMapper;
import com.rickey.core.mapper.UserInterfaceInfoMapper;
import com.rickey.core.service.UserInterfaceInfoService;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.entity.UserInterfaceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 用户接口信息服务实现类
 */
@Service
@Slf4j
public class UserInterfaceInfoServiceImpl extends ServiceImpl<UserInterfaceInfoMapper, UserInterfaceInfo>
        implements UserInterfaceInfoService {

    private final UserInterfaceInfoMapper userInterfaceInfoMapper;

    private final InterfaceInfoMapper interfaceInfoMapper;

    @Autowired
    public UserInterfaceInfoServiceImpl(UserInterfaceInfoMapper userInterfaceInfoMapper, InterfaceInfoMapper interfaceInfoMapper) {
        this.userInterfaceInfoMapper = userInterfaceInfoMapper;
        this.interfaceInfoMapper = interfaceInfoMapper;
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
    @Transactional
    public boolean updateLeftNum(long interfaceInfoId, long userId, int leftNum, int increment) {
        log.info("updateLeftNum方法调用:interfaceInfoId={}, userId={}", interfaceInfoId, userId);
        if (interfaceInfoId == 0 || userId <= 0) {
            log.info("interfaceInfoId非法或者userId非法");
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        log.info("updateLeftNum方法调用...");
        UserInterfaceInfo userInterfaceInfo = getUserInterfaceInfo(userId, interfaceInfoId);
        log.info("userInterfaceInfo = {}", userInterfaceInfo);
        if (userInterfaceInfo == null) {
            // 创建对应关系
            log.info("找不到对应的信息，新建中...");
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
            log.info("接口调用次数更新成功");
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


    @Override
    public boolean applyForApiCallIncrease(Long userId, Long interfaceInfoId, Integer invokeCount) {
        // 1. 获取接口信息
        InterfaceInfo interfaceInfo = interfaceInfoMapper.selectById(interfaceInfoId);
        if (Objects.isNull(interfaceInfo)) {
            // 如果接口信息不存在，返回false
            log.warn("InterfaceInfo not found, interfaceInfoId: {}", interfaceInfoId);
            return false;
        }

        log.info("InterfaceInfo found: {}", interfaceInfo);

        // 2. 查找用户接口信息
        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("interfaceInfoId", interfaceInfoId);
        UserInterfaceInfo userInterfaceInfo = userInterfaceInfoMapper.selectOne(queryWrapper);

        // 3. 如果用户接口信息不存在，创建新的记录
        if (userInterfaceInfo == null) {
            log.info("UserInterfaceInfo not found for userId: {}, interfaceInfoId: {}, creating new record.", userId, interfaceInfoId);

            userInterfaceInfo = new UserInterfaceInfo();
            userInterfaceInfo.setUserId(userId);
            userInterfaceInfo.setInterfaceInfoId(interfaceInfoId);
            userInterfaceInfo.setLeftNum(invokeCount); // 初始化剩余次数
            int insertCount = userInterfaceInfoMapper.insert(userInterfaceInfo);

            if (insertCount > 0) {
                log.info("Successfully created UserInterfaceInfo for userId: {}, interfaceInfoId: {}", userId, interfaceInfoId);
                return true;
            } else {
                log.error("Failed to create UserInterfaceInfo for userId: {}, interfaceInfoId: {}", userId, interfaceInfoId);
                return false;
            }
        }

        // 4. 增加调用次数
        Integer oldLeftNum = userInterfaceInfo.getLeftNum();
        Integer newLeftNum = oldLeftNum + invokeCount;
        log.debug("Old leftNum: {}, New leftNum: {}", oldLeftNum, newLeftNum);

        if (oldLeftNum >= 100 || newLeftNum < 0) {
            // 如果原本已经有100次调用次数了或者增加后的调用次数小于0，可能是非法操作，返回false
            log.warn("Invalid operation, oldLeftNum: {}, newLeftNum: {}", oldLeftNum, newLeftNum);
            return false;
        }

        // 5. 更新用户接口信息
        UpdateWrapper<UserInterfaceInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("leftNum", newLeftNum)  // 更新剩余次数
                .eq("userId", userId)
                .eq("interfaceInfoId", interfaceInfoId);
        int updateCount = userInterfaceInfoMapper.update(null, updateWrapper);

        if (updateCount > 0) {
            log.info("Successfully updated leftNum for userId: {}, interfaceInfoId: {}", userId, interfaceInfoId);
        } else {
            log.error("Failed to update leftNum for userId: {}, interfaceInfoId: {}", userId, interfaceInfoId);
        }

        return updateCount > 0;  // 如果更新成功，返回true，否则返回false
    }



}




