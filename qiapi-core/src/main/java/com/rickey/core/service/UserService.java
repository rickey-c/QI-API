package com.rickey.core.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.rickey.common.model.entity.User;
import com.rickey.core.model.vo.UserDevKeyVO;
import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

/**
 * 用户服务
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param email   用户邮箱
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param code 验证码
     * @return 新用户 id
     */
    long userRegister(String email, String userPassword, String checkPassword, String code);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword);


    /**
     * 是否为管理员
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 用户注销
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 通过token获取用户信息
     *
     * @param token
     * @return
     */
    User getUserByToken(String token);

    /**
     * 发送邮箱
     * @param email
     * @throws MessagingException
     * @throws UnsupportedEncodingException
     */
    void sendEmail(String email) throws MessagingException, UnsupportedEncodingException;

    /**
     * 重新生成ak，sk
     *
     * @param request
     * @return
     */
    UserDevKeyVO genKey(HttpServletRequest request);
}
