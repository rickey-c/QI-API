package com.rickey.backend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rickey.backend.mapper.UserInterfaceInfoMapper;
import com.rickey.backend.model.vo.InterfaceInfoVO;
import com.rickey.backend.service.InterfaceInfoService;
import com.rickey.common.model.entity.InterfaceInfo;
import com.rickey.common.model.entity.UserInterfaceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InterfaceStatisticsTask {

    private final UserInterfaceInfoMapper userInterfaceInfoMapper;

    private final InterfaceInfoService interfaceInfoService;

    // 定义一个集合来存储计算结果
    private static final Map<Long, InterfaceInfoVO> cachedInterfaceInfoVOMap = new ConcurrentHashMap<>();

    public InterfaceStatisticsTask(UserInterfaceInfoMapper userInterfaceInfoMapper, InterfaceInfoService interfaceInfoService) {
        this.userInterfaceInfoMapper = userInterfaceInfoMapper;
        this.interfaceInfoService = interfaceInfoService;
    }

    // 每天凌晨2点执行任务
    @Scheduled(cron = "0 0 3 * * ?")
    public void calculateTopInvokeInterfaceInfo() {
        // 执行原方法中的逻辑来获取和计算接口调用次数最多的接口信息列表
        List<UserInterfaceInfo> userInterfaceInfoList = userInterfaceInfoMapper.listTopInvokeInterfaceInfo(3);
        Map<Long, List<UserInterfaceInfo>> interfaceInfoIdObjMap = userInterfaceInfoList.stream()
                .collect(Collectors.groupingBy(UserInterfaceInfo::getInterfaceInfoId));

        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", interfaceInfoIdObjMap.keySet());
        List<InterfaceInfo> list = interfaceInfoService.list(queryWrapper);

        if (!CollectionUtils.isEmpty(list)) {
            List<InterfaceInfoVO> interfaceInfoVOList = list.stream().map(interfaceInfo -> {
                InterfaceInfoVO interfaceInfoVO = new InterfaceInfoVO();
                BeanUtils.copyProperties(interfaceInfo, interfaceInfoVO);
                int totalNum = interfaceInfoIdObjMap.get(interfaceInfo.getId()).get(0).getTotalNum();
                interfaceInfoVO.setTotalNum(totalNum);
                return interfaceInfoVO;
            }).collect(Collectors.toList());

            // 更新缓存
            cachedInterfaceInfoVOMap.clear();
            interfaceInfoVOList.forEach(vo -> cachedInterfaceInfoVOMap.put(vo.getId(), vo));
            log.info("更新之后的列表 = {}", interfaceInfoVOList);
        }
    }

    public static Map<Long, InterfaceInfoVO> getCachedInterfaceInfoVOMap() {
        return cachedInterfaceInfoVOMap;
    }
}
