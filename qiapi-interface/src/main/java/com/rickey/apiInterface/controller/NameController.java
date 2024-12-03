package com.rickey.apiInterface.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.rickey.apiInterface.config.SentinelConfig;
import com.rickey.clientSDK.model.User;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 名称 API
 */
@RestController
@RequestMapping("/name")
public class NameController {


    @PostMapping("/user")
    @SentinelResource(value = "qi-api-interface",
            blockHandler = "blockHandlerPOST", blockHandlerClass = SentinelConfig.class,
            fallback = "fallbackPOST", fallbackClass = SentinelConfig.class)
    public String getUsernameByPost(@RequestBody User user, HttpServletRequest request) {
        String result = "POST 用户名字是" + user.getUsername();
        return result;
    }
}
