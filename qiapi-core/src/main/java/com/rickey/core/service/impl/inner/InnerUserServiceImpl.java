package com.rickey.core.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rickey.core.mapper.UserMapper;
import com.rickey.core.service.UserService;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.entity.User;
import com.rickey.common.service.InnerUserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 内部用户服务实现类
 */
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

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
     * @param token
     * @return
     */
    @Override
    public User getUserByToken(String token) {
        return userService.getUserByToken(token);
    }


}
