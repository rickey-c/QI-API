package com.rickey.apiInterface.config;


import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
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
        // 配置NameController规则
        List<FlowRule> rules = new ArrayList<>();
        // 规则名称
        FlowRule NameController = new FlowRule();
        // 资源名称
        NameController.setResource("NameController");
        // 策略:基于QPS的流量控制
        NameController.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // Set limit QPS to 300.
        NameController.setCount(300);
        // 添加规则
        rules.add(NameController);

        // 规则名称
        FlowRule RandomController = new FlowRule();
        // 资源名称
        RandomController.setResource("RandomController");
        // 策略:基于QPS的流量控制
        RandomController.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // Set limit QPS to 200,需要访问到数据库，流量控制得小一点.
        RandomController.setCount(200);
        // 添加规则
        rules.add(RandomController);
        // Manager加载规则
        FlowRuleManager.loadRules(rules);
    }



}
