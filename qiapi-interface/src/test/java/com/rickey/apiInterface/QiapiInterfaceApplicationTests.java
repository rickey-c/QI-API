package com.rickey.apiInterface;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rickey.apiInterface.model.entity.City;
import com.rickey.apiInterface.service.CityService;
import com.rickey.apiInterface.util.IpUtil;
import com.rickey.common.common.ErrorCode;
import com.rickey.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 测试类
 */
@SpringBootTest
@Slf4j
class QiapiInterfaceApplicationTests {

    @Autowired
    private CityService cityService;

    @Test
    void IpTest() throws UnknownHostException {
        String ip = "112.48.20.177";
        InetAddress inetAddress = InetAddress.getByName(ip);
        log.info("inetAddress = {}", inetAddress);
        if (inetAddress instanceof Inet4Address) {
            String location = IpUtil.getIpLocation(ip);
            log.info("location = {}", location);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不是合法的IPv4地址!");
        }
    }

    @Test
    void cityTest() {
        City city = cityService.query().eq("name", "北京市").one();
        log.info("city = {}", city);
        BaseMapper<City> baseMapper = cityService.getBaseMapper();
        log.info("baseMapper = {}", baseMapper);
    }

}
