package com.rickey.clientSDK.client;

import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class RandomApiClient extends CommonApiClient {

    public RandomApiClient(String accessKey, String secretKey) {
        super(accessKey, secretKey);
    }

    public String getRandomEncouragement() {
        return HttpRequest.get(GATEWAY_HOST + "/api/interfaceInvoke/random/encouragement")
                .addHeaders(getHeadMap(null, accessKey, secretKey))
                .execute().body();
    }

    public String getRandomImageUrl() {
        return HttpRequest.get(GATEWAY_HOST + "/api/interfaceInvoke/random/image")
                .addHeaders(getHeadMap(null, accessKey, secretKey))
                .execute().body();
    }
}
