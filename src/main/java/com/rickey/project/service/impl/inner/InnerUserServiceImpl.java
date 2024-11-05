package com.rickey.project.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rickey.project.exception.BusinessException;
import com.rickey.project.mapper.UserMapper;
import com.rickey.project.common.ErrorCode;
import com.rickey.qiapicommon.model.entity.User;
import com.rickey.qiapicommon.service.InnerUserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/**
 * 内部用户服务实现类
 */
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public User getInvokeUser(String accessKey) {
        if (StringUtils.isAnyBlank(accessKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("accessKey", accessKey);
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * @param userId
     * @return
     */
    @Override
    public Boolean updateInvokeCount(long userId) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", userId);
        User user = userMapper.selectOne(queryWrapper);
        if (user != null) {
            user.setRemainingCalls(user.getRemainingCalls() - 1);
            userMapper.updateById(user);
            return true;
        } else {
            return false;
        }
    }
}
