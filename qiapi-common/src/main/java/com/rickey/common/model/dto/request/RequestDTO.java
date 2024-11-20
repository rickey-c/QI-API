package com.rickey.common.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.servlet.http.Cookie;
import java.io.Serializable;

/**
 * @Author: Rickey
 * @CreateTime: 2024-11-19
 * @Description: 用于封装请求参数，实现远程调用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestDTO implements Serializable {
    private Cookie[] cookies;
}