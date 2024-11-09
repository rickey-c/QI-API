package com.rickey.backend;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.rickey.backend.mapper")
@EnableDubbo
@EnableCaching
public class QiapiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(QiapiBackendApplication.class, args);
    }

}
