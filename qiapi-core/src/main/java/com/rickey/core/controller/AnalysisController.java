package com.rickey.core.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rickey.core.job.InterfaceStatisticsTask;
import com.rickey.core.mapper.UserInterfaceInfoMapper;
import com.rickey.core.model.vo.InterfaceInfoVO;
import com.rickey.core.service.InterfaceInfoService;
import com.rickey.common.annotation.AuthCheck;
import com.rickey.common.common.BaseResponse;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.entity.InterfaceInfo;
import com.rickey.common.model.entity.UserInterfaceInfo;
import com.rickey.common.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分析控制器
 */
@RestController
@RequestMapping("/analysis")
@Slf4j
public class AnalysisController {

    private final UserInterfaceInfoMapper userInterfaceInfoMapper;

    private final InterfaceStatisticsTask interfaceStatisticsTask;

    private final InterfaceInfoService interfaceInfoService;

    @Autowired
    public AnalysisController(UserInterfaceInfoMapper userInterfaceInfoMapper, InterfaceInfoService interfaceInfoService, InterfaceStatisticsTask interfaceStatisticsTask) {
        this.userInterfaceInfoMapper = userInterfaceInfoMapper;
        this.interfaceInfoService = interfaceInfoService;
        this.interfaceStatisticsTask = interfaceStatisticsTask;
    }

    /**
     * 获取调用次数最多的接口信息列表
     *
     * @return
     */

    /**
     * 获取调用次数最多的接口信息列表。
     * 通过用户接口信息表查询调用次数最多的接口ID，再关联查询接口详细信息。
     *
     * @return 接口信息列表，包含调用次数最多的接口信息
     */
    @GetMapping("/top/interface/invoke")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<List<InterfaceInfoVO>> listTopInvokeInterfaceInfo() {

        // 拿到缓存的结果
        Map<Long, InterfaceInfoVO> cachedMap = InterfaceStatisticsTask.getCachedInterfaceInfoVOMap();
        log.info("缓存结果 = {}", cachedMap);
        // 如果缓存不存在，就正常走数据库
        if (cachedMap.isEmpty()) {
            // 查询调用次数最多的接口信息列表
            List<UserInterfaceInfo> userInterfaceInfoList = userInterfaceInfoMapper.listTopInvokeInterfaceInfo(3);
            // 将接口信息按照接口ID分组，便于关联查询
            Map<Long, List<UserInterfaceInfo>> interfaceInfoIdObjMap = userInterfaceInfoList.stream()
                    .collect(Collectors.groupingBy(UserInterfaceInfo::getInterfaceInfoId));

            // 调试
            for (Map.Entry<Long, List<UserInterfaceInfo>> entry : interfaceInfoIdObjMap.entrySet()) {
                Long interfaceId = entry.getKey();
                List<UserInterfaceInfo> interfaceInfoList = entry.getValue();
                System.out.println("接口 ID: " + interfaceId);
                System.out.println("对应的接口信息列表: " + interfaceInfoList);
                System.out.println("--------------");
            }

            // 创建查询接口信息的条件包装器
            QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
            // 设置查询条件，使用接口信息ID在接口信息映射中的键集合进行条件匹配
            queryWrapper.in("id", interfaceInfoIdObjMap.keySet());
            // 调用接口信息服务的list方法，传入条件包装器，获取符合条件的接口信息列表
            List<InterfaceInfo> list = interfaceInfoService.list(queryWrapper);
            // 判断查询结果是否为空
            if (CollectionUtils.isEmpty(list)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            // 构建接口信息VO列表，使用流式处理将接口信息映射为接口信息VO对象，并加入列表中
            List<InterfaceInfoVO> interfaceInfoVOList = list.stream().map(interfaceInfo -> {
                // 创建一个新的接口信息VO对象
                InterfaceInfoVO interfaceInfoVO = new InterfaceInfoVO();
                // 将接口信息复制到接口信息VO对象中
                BeanUtils.copyProperties(interfaceInfo, interfaceInfoVO);
                // 从接口信息ID对应的映射中获取调用次数
                int totalNum = interfaceInfoIdObjMap.get(interfaceInfo.getId()).get(0).getTotalNum();
                // 将调用次数设置到接口信息VO对象中
                interfaceInfoVO.setTotalNum(totalNum);
                // 返回构建好的接口信息VO对象
                return interfaceInfoVO;
            }).collect(Collectors.toList());
            // 手动调用更新缓存
            interfaceStatisticsTask.calculateTopInvokeInterfaceInfo();
            log.info("查询不到缓存,在查询数据库后添加缓存");
            // 返回处理结果
            return ResultUtils.success(interfaceInfoVOList);
        }

        List<InterfaceInfoVO> interfaceInfoVOList = new ArrayList<>(cachedMap.values());
        // 根据totalNum降序排序
        interfaceInfoVOList.sort(Comparator.comparingInt(InterfaceInfoVO::getTotalNum).reversed());
        return ResultUtils.success(interfaceInfoVOList);
    }

    @GetMapping("/updateCache")
    public BaseResponse<String> testUpdateCache() {
        // 手动调用定时任务的方法来更新缓存
        interfaceStatisticsTask.calculateTopInvokeInterfaceInfo();
        return ResultUtils.success("缓存已更新");
    }

}

