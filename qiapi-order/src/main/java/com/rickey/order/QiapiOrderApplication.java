package com.rickey.order;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.rickey.order.mapper")
@EnableDubbo
@EnableCaching
public class QiapiOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(QiapiOrderApplication.class, args);
    }

}
