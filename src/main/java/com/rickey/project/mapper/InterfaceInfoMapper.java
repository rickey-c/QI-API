package com.rickey.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rickey.qiapicommon.model.entity.InterfaceInfo;

import java.util.List;

/**
 * 接口信息 Mapper
 */
public interface InterfaceInfoMapper extends BaseMapper<InterfaceInfo> {

    List<InterfaceInfo> listTopInvokeInterfaceInfo(int i);
}

