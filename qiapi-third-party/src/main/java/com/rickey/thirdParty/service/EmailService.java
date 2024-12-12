package com.rickey.thirdParty.service;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

public interface EmailService {
    String sendMail(String sendTo) throws MessagingException, UnsupportedEncodingException;
}
