package com.rickey.core;

import com.rickey.clientSDK.client.RandomApiClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@Slf4j
@SpringBootTest
class QiapiBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void clientTest() {
        RandomApiClient randomApiClient = new RandomApiClient("0c2f5c75abcdfe57ead46b409a332cbc", "f6e07ff8cca83a9a3b24fd20960e14e3");
        String randomEncouragement = randomApiClient.getRandomEncouragement();
        log.info("randomEncouragement = {}", randomEncouragement);
    }

}
