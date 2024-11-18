package com.rickey.order;

import com.rickey.common.service.InnerUserService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class QiapiOrderApplicationTests {

    @DubboReference
    InnerUserService innerUserService;

    @Test
    void contextLoads() {
        innerUserService.getInvokeUser("");
    }

}
