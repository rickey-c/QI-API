package com.rickey.core;

import com.rickey.clientSDK.client.RandomApiClient;
import com.rickey.common.service.InnerEmailService;
import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.UnsupportedEncodingException;


@Slf4j
@SpringBootTest
class QiapiBackendApplicationTests {

    @DubboReference
    private InnerEmailService emailService;

    @Test
    void clientTest() {
        RandomApiClient randomApiClient = new RandomApiClient("0c2f5c75abcdfe57ead46b409a332cbc", "f6e07ff8cca83a9a3b24fd20960e14e3");
        String randomEncouragement = randomApiClient.getRandomEncouragement();
        log.info("randomEncouragement = {}", randomEncouragement);
    }


    @Test
    void sendEmailDubboTest() {
        String code = null;
        try {
            code = emailService.sendEmail("rickeychen137@gmail.com");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        log.info("code = {}", code);
    }

}
