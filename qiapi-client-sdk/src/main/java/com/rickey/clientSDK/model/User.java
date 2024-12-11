package com.rickey.clientSDK.model;

import lombok.Data;

/**
 * 用户类，用于封装用户信息。
 *
 * 该类包含以下字段：
 * <ul>
 *     <li>username：用户名</li>
 * </ul>
 *
 * @author rickey-c
 */
@Data
public class User {
    /**
     * 用户名
     * <p>
     * 该字段用于存储用户的用户名。
     */
    private String username;
}
