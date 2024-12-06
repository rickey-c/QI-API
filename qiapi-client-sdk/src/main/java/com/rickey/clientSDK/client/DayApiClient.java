package com.rickey.clientSDK.client;

import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DayApiClient extends CommonApiClient {
    public DayApiClient(String accessKey, String secretKey) {
        super(accessKey, secretKey);
    }

    public String getDayWallpaperUrl() {
        return HttpRequest.get(GATEWAY_HOST + "/api/interfaceInvoke/day/wallpaper")
                .addHeaders(getHeadMap(null, accessKey, secretKey))
                .execute().body();
    }
}
