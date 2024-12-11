package com.rickey.clientSDK.client;

import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 随机 API 客户端，用于调用与随机内容相关的接口，例如随机鼓励语和随机图片 URL。
 */
@Slf4j
public class RandomApiClient extends CommonApiClient {

    /**
     * 构造函数，用于初始化 `RandomApiClient` 实例。
     *
     * @param accessKey 开发者的访问密钥
     * @param secretKey 开发者的安全密钥
     */
    public RandomApiClient(String accessKey, String secretKey) {
        super(accessKey, secretKey);
    }

    /**
     * 获取随机鼓励语。
     *
     * @return 随机鼓励语的字符串
     */
    public String getRandomEncouragement() {
        return HttpRequest.get(GATEWAY_HOST + "/api/interfaceInvoke/random/encouragement")
                .addHeaders(getHeadMap(null, accessKey, secretKey))
                .execute().body();
    }

    /**
     * 获取随机图片的 URL。
     *
     * @return 随机图片的 URL 字符串
     */
    public String getRandomImageUrl() {
        return HttpRequest.get(GATEWAY_HOST + "/api/interfaceInvoke/random/image")
                .addHeaders(getHeadMap(null, accessKey, secretKey))
                .execute().body();
    }
}
