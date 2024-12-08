package com.rickey.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rickey.common.model.entity.UserInterfaceInfo;

import java.util.List;

/**
 * 用户接口信息 Mapper
 */
public interface UserInterfaceInfoMapper extends BaseMapper<UserInterfaceInfo> {

    List<UserInterfaceInfo> listTopInvokeInterfaceInfo(int limit);

    boolean updateLeftNumByIncrement(Long id, int leftNum, int increment);
}




