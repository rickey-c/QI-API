package com.rickey.clientSDK.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.rickey.clientSDK.model.User;

public class NameApiClient extends CommonApiClient {
    public NameApiClient(String accessKey, String secretKey) {
        super(accessKey, secretKey);
    }

    public String getUserNameByPost(User user) {
        String json = JSONUtil.toJsonStr(user);
        return HttpRequest.post(GATEWAY_HOST + "/api/interfaceInvoke/name/user")
                .addHeaders(getHeadMap(json, accessKey, secretKey))
                .body(json)
                .execute()
                .body();
    }
}
