package com.rickey.common.service;

import com.rickey.common.model.entity.User;
import javax.servlet.http.HttpServletRequest;


/**
 * 内部用户服务
 */
public interface InnerUserService {

    /**
     * 数据库中查是否已分配给用户秘钥（accessKey）
     *
     * @param accessKey
     * @return
     */
    User getInvokeUser(String accessKey);

    User getLoginUser(HttpServletRequest request);

    boolean isAdmin(HttpServletRequest request);
}
