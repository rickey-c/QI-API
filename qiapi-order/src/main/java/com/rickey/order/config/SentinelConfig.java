package com.rickey.order.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import com.rickey.common.common.BaseResponse;
import com.rickey.common.utils.ResultUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelConfig {

    @PostConstruct
    public void init() {
        // 初始化流控规则
        initFlowRules();
    }

    private static void initFlowRules() {
        // 配置getRandomEncouragement规则
        List<FlowRule> rules = new ArrayList<>();
        // 规则名称
        FlowRule getRandomEncouragement = new FlowRule();
        // 资源名称
        getRandomEncouragement.setResource("qi-api-interface");
        // 策略:基于QPS的流量控制
        getRandomEncouragement.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // Set limit QPS to 20.
        getRandomEncouragement.setCount(20);
        // 添加规则
        rules.add(getRandomEncouragement);
        FlowRuleManager.loadRules(rules);
    }

    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    // 方法出现异常处理方
    public static BaseResponse<Object> fallbackGET(HttpServletRequest request, Throwable throwable) {
        // 处理降级逻辑，比如返回默认值
        System.out.println("降级处理: " + throwable.getMessage());
        return ResultUtils.error(505, "服务暂时不可用，请稍后再试。");
    }

    // 流量超出规则处理方法
    public static BaseResponse<Object> blockHandlerGET(HttpServletRequest request, BlockException blockException) {
        // 处理流量控制逻辑
        System.out.println("流量控制触发");
        return ResultUtils.error(506, "请求过于频繁，请稍后再试。");
    }


//    // POST方法出现异常处理方
//    public static BaseResponse<Object> fallbackPOST(InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
//                                                    HttpServletRequest request, Throwable throwable) {
//        // 处理降级逻辑，比如返回默认值
//        System.out.println("降级处理: " + throwable.getMessage());
//        return ResultUtils.error(505, "服务暂时不可用，请稍后再试。");
//    }
//
//    // POST流量超出规则处理方法
//    public static BaseResponse<Object> blockHandlerPOST(InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
//                                                        HttpServletRequest request, BlockException blockException) {
//        // 处理流量控制逻辑
//        System.out.println("流量控制触发");
//        return ResultUtils.error(506, "请求过于频繁，请稍后再试。");
//    }

}
