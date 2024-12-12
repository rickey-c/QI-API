package com.rickey.thirdParty;

import com.rickey.thirdParty.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

@Slf4j
@SpringBootTest
class QiapiThirdPartyApplicationTests {

    @Autowired
    private EmailService emailService;

    @Test
    void sendEmail() throws MessagingException, UnsupportedEncodingException {
        String code = emailService.sendMail("rickeychen137@gmail.com");
        log.info("send Email : {}", code);
    }

}
