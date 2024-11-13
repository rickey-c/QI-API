package com.rickey.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.rickey.order.mapper")
public class QiapiOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(QiapiOrderApplication.class, args);
    }

}
