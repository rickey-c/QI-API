package com.rickey.apiInterface;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * QiApi 模拟接口入口类
 */
@SpringBootApplication
@MapperScan("com.rickey.apiInterface.mapper")
@EnableDiscoveryClient
public class QiApiInterfaceInvokeApplication {

    public static void main(String[] args) {
        SpringApplication.run(QiApiInterfaceInvokeApplication.class, args);
    }

}
