package com.rickey.common.service;

import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException;

import java.io.UnsupportedEncodingException;

public interface InnerEmailService {
    String sendEmail(String sendTo) throws MessagingException, UnsupportedEncodingException;
}
