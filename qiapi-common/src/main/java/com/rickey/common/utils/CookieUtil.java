package com.rickey.common.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class CookieUtil {

    private final static String COOKIE_NAME = "rickey_login_token";
    private final static String COOKIE_DOMAIN = "rickey.com";

    /**
     * 写cookie
     *
     * @param token    就是sessionid，也就是cookie的值，这个值只要唯一就行了，使用UUID也可以
     * @param response 使用响应对象将cookie写到浏览器上
     */
    public static void writeLoginToken(String token, HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setDomain(COOKIE_DOMAIN);
        cookie.setPath("/");
        //设置生存时间.0无效，-1永久有效，时间是秒，生存时间设置为1h
        cookie.setMaxAge(60 * 60);
        //设置安全机制
        // cookie.setHttpOnly(true);
        log.info("写 cookie name：{},value：{}", cookie.getName(), cookie.getValue());
        //将cookie写到浏览器上
        response.addCookie(cookie);
    }

    /**
     * 读取cookie
     *
     * @param request
     * @return
     */
    public static String readLoginToken(HttpServletRequest request) {
        //获取cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            //遍历cookie，取出我们自己的cookie，根据name获取
            for (Cookie cookie : cookies) {
                log.info("读取cookie cookieName{},cookieValue{}", cookie.getName(), cookie.getValue());
                //获取自己的
                if (StrUtil.equals(cookie.getName(), COOKIE_NAME)) {
                    //获取值
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 删除cookie
     *
     * @param request
     * @param response
     */
    public static void deleteLoginToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (StrUtil.equals(cookie.getName(), COOKIE_NAME)) {
                    //设置cookie的有效期为0
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    cookie.setDomain(COOKIE_DOMAIN);
                    log.info("删除cookie cookieName: {},cookieValue: {}", cookie.getName(), cookie.getValue());
                    response.addCookie(cookie);
                    return;
                }
            }
        }
    }
}
