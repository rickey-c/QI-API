package com.rickey.clientSDK.client;


import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.rickey.clientSDK.model.User;

import static com.rickey.clientSDK.utils.HeaderUtils.getHeaderMap;

/**
 * 调用第三方接口的客户端
 */
public class QiApiClient {

    private static final String GATEWAY_HOST = "http://localhost:8090";

    private String accessKey;

    private String secretKey;

    public QiApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getUsernameByPost(User user) {
        String json = JSONUtil.toJsonStr(user);
        HttpResponse httpResponse = HttpRequest.post(GATEWAY_HOST + "/api/interfaceInvoke/name/user")
                .addHeaders(getHeaderMap(accessKey, secretKey, json))
                .body(json)
                .execute();
        System.out.println(httpResponse.getStatus());
        String result = httpResponse.body();
        System.out.println(result);
        return result;
    }

    // TODO 这里由于没有token会被认为未登录
    public String getRandomEncouragement() {
        HttpResponse httpResponse = HttpRequest.get(GATEWAY_HOST + "/api/interfaceInvoke/random/encouragement")
                .addHeaders(getHeaderMap(accessKey, secretKey))
                .execute();
        System.out.println(httpResponse.getStatus());
        String result = httpResponse.body();
        System.out.println(result);
        return result;
    }
}
