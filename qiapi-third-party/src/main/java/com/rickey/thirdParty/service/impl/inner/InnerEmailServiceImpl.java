package com.rickey.thirdParty.service.impl.inner;

import com.rickey.common.service.InnerEmailService;
import com.rickey.thirdParty.service.EmailService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

@DubboService
public class InnerEmailServiceImpl implements InnerEmailService {

    @Autowired
    private EmailService emailService;

    /**
     * @param sendTo
     * @return
     */
    @Override
    public String sendEmail(String sendTo) throws UnsupportedEncodingException {
        try {
            String code = emailService.sendMail(sendTo);
            return code;
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
