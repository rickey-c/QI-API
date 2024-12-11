package com.rickey.clientSDK.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.rickey.clientSDK.model.User;
import lombok.extern.slf4j.Slf4j;

/**
 * 名称 API 客户端，用于处理与用户名称相关的接口调用。
 */
@Slf4j
public class NameApiClient extends CommonApiClient {

    /**
     * 构造函数，用于初始化 `NameApiClient` 实例。
     *
     * @param accessKey 开发者的访问密钥
     * @param secretKey 开发者的安全密钥
     */
    public NameApiClient(String accessKey, String secretKey) {
        super(accessKey, secretKey);
    }

    /**
     * 通过 POST 请求获取用户的名称。
     *
     * @param user 用户对象，包含需要传递的用户信息
     * @return 用户名称的 JSON 字符串
     */
    public String getUserNameByPost(User user) {
        // 将用户对象序列化为 JSON 字符串
        String json = JSONUtil.toJsonStr(user);
        log.info("json = {}", json);
        // 发送 POST 请求并返回响应体
        return HttpRequest.post(GATEWAY_HOST + "/api/interfaceInvoke/name/user")
                .addHeaders(getHeadMap(json, accessKey, secretKey))
                .body(json)
                .execute()
                .body();
    }
}
