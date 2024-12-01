package com.rickey.thirdParty;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class QiapiThirdPartyApplication {

    public static void main(String[] args) {
        SpringApplication.run(QiapiThirdPartyApplication.class, args);
    }

}
