package com.rickey.apiInterface.controller;


import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.rickey.apiInterface.config.SentinelConfig;
import com.rickey.apiInterface.service.EncouragementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 随机 API
 */
@RestController
@RequestMapping("/random")
@Slf4j
public class RandomController {

    @Resource
    private EncouragementService encouragementService;

    @GetMapping("/encouragement")
    @SentinelResource(value = "qi-api-interface",
            blockHandler = "blockHandlerGET", blockHandlerClass = SentinelConfig.class,
            fallback = "fallbackGET", fallbackClass = SentinelConfig.class)
    public String getRandomEncouragement(HttpServletRequest request) {
        log.info(request.getRequestURI());
        String result = "心灵鸡汤为: " + encouragementService.getRandomEncouragement().getMessage();
        System.out.println(result);
        return result;
    }
}
