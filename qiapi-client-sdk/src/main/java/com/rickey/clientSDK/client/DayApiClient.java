package com.rickey.clientSDK.client;

import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 日间 API 客户端，用于调用与每日功能相关的 API，例如获取每日壁纸 URL。
 */
@Slf4j
public class DayApiClient extends CommonApiClient {

    /**
     * 构造函数，用于初始化 `DayApiClient` 实例。
     *
     * @param accessKey 开发者的访问密钥
     * @param secretKey 开发者的安全密钥
     */
    public DayApiClient(String accessKey, String secretKey) {
        super(accessKey, secretKey);
    }

    /**
     * 获取每日壁纸的 URL 地址。
     *
     * @return 每日壁纸的 URL 字符串
     */
    public String getDayWallpaperUrl() {
        return HttpRequest.get(GATEWAY_HOST + "/api/interfaceInvoke/day/wallpaper")
                .addHeaders(getHeadMap(null, accessKey, secretKey))
                .execute().body();
    }
}
