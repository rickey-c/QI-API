package com.rickey.qiapiinterface;

import com.rickey.qiapiclientsdk.client.QiApiClient;
import com.rickey.qiapiclientsdk.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 测试类
 */
@SpringBootTest
class QiapiInterfaceApplicationTests {

    @Resource
    private QiApiClient qiApiClient;


}
