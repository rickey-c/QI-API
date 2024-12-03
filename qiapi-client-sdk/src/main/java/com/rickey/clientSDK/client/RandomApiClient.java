package com.rickey.clientSDK.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class RandomApiClient extends CommonApiClient {

    public RandomApiClient(String accessKey, String secretKey) {
        super(accessKey, secretKey);
    }

    public String getRandomEncouragement() {
        HttpResponse httpResponse = HttpRequest.get(GATEWAY_HOST + "/api/interfaceInvoke/random/encouragement")
                .addHeaders(getHeadMap(null, accessKey, secretKey))
                .execute();
        System.out.println(httpResponse.getStatus());
        String result = httpResponse.body();
        System.out.println(result);
        return result;
    }
}
