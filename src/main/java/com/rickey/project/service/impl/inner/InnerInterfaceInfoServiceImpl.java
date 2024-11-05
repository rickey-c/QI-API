package com.rickey.project.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rickey.project.common.ErrorCode;
import com.rickey.project.exception.BusinessException;
import com.rickey.project.mapper.InterfaceInfoMapper;
import com.rickey.qiapicommon.model.entity.InterfaceInfo;
import com.rickey.qiapicommon.service.InnerInterfaceInfoService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/**
 * 内部接口服务实现类
 */
@DubboService
public class InnerInterfaceInfoServiceImpl implements InnerInterfaceInfoService {

    @Resource
    private InterfaceInfoMapper interfaceInfoMapper;

    @Override
    public InterfaceInfo getInterfaceInfo(String url, String method) {
        if (StringUtils.isAnyBlank(url, method)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("url", url);
        queryWrapper.eq("method", method);
        return interfaceInfoMapper.selectOne(queryWrapper);
    }

    /**
     * @param interfaceInfoId
     * @return
     */
    @Override
    public Boolean updateInvokeCount(long interfaceInfoId) {
        QueryWrapper<InterfaceInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("id", interfaceInfoId);
        InterfaceInfo interfaceInfo = interfaceInfoMapper.selectOne(wrapper);
        if (interfaceInfo != null) {
            interfaceInfo.setInvokedCount(interfaceInfo.getInvokedCount() + 1);
            interfaceInfoMapper.updateById(interfaceInfo);
            return true;
        } else {
            return false;
        }
    }

}
