package com.rickey.apiInterface.controller;

import com.rickey.apiInterface.util.IpUtil;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.exception.BusinessException;
import com.rickey.common.model.dto.request.IpAnaParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@RestController
@RequestMapping("/ip")
public class IpController {

    @PostMapping("/analysis/target")
    public String getTargetIpAnalysis(@RequestBody IpAnaParam param) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(param.getIp());
        log.info("inetAddress = {}", inetAddress);
        if (inetAddress instanceof Inet4Address) {
            String location = IpUtil.getIpLocation(param.getIp());
            log.info("location = {}", location);
            return location;
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不是合法的IPv4地址!");
        }
    }
}

