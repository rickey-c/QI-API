package com.rickey.gateway.config;

import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;

/**
 * Sentinel 限流熔断规则管理器
 */
@Component
public class SentinelConfig {
    @PostConstruct
    public void initRules() throws Exception {
        initFlowRules();

    }

    /**
     * 初始化限流规则
     */
    public void initFlowRules() {
        // 单 IP 查看题目列表限流规则
        ParamFlowRule rule = new ParamFlowRule("IP-Rule")
                .setParamIdx(0) // 对第 0 个参数限流，即 IP 地址
                .setCount(5) // 每分钟最多 60 次
                .setDurationInSec(40); // 规则的统计周期为 60 秒
        ParamFlowRuleManager.loadRules(Collections.singletonList(rule));
    }

}