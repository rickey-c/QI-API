package com.rickey.thirdParty.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.rickey.thirdParty.service.EmailService;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

@Service
public class EmailServiceImpl implements EmailService {

    final JavaMailSender javaMailSender;

    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * @param sendTo
     * @return
     */
    @Override
    public String sendMail(String sendTo) throws MessagingException, UnsupportedEncodingException {
        // 创建一个邮件消息
        MimeMessage message = javaMailSender.createMimeMessage();

        // 创建 MimeMessageHelper
        MimeMessageHelper helper = new MimeMessageHelper(message, false);

        // 发件人邮箱和名称
        helper.setFrom("1604120019@qq.com", "Qi-API 接口开放平台");
        // 收件人邮箱
        helper.setTo(sendTo);
        // 邮件标题
        helper.setSubject("Hello");
        String code = String.valueOf(RandomUtil.randomLong(100000, 1000000));
        String content = "【Qi-API 接口开放平台】您好，您的验证码是：" + code + "，五分钟之内有效。请勿泄露！";
        // 邮件正文，第二个参数表示是否是HTML正文
        helper.setText(content);

        // 发送
        javaMailSender.send(message);
        return code;
    }
}
