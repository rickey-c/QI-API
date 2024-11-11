package com.rickey.apiInterface;

import com.rickey.clientSDK.client.QiApiClient;
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
