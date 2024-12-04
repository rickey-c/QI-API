package com.rickey.apiInterface.controller;


import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.rickey.apiInterface.sentinel.BlockHandlerService;
import com.rickey.apiInterface.sentinel.FallbackService;
import com.rickey.apiInterface.service.EncouragementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

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
    @SentinelResource(value = "RandomController",
            blockHandler = "RandomControllerBlockHandler", blockHandlerClass = BlockHandlerService.class,
            fallback = "RandomControllerFallback", fallbackClass = FallbackService.class)
    public String getRandomEncouragement() {
        return "心灵鸡汤为: " + encouragementService.getRandomEncouragement().getMessage();
    }


}
