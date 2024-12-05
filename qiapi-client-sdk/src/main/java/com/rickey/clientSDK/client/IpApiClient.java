package com.rickey.clientSDK.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.rickey.clientSDK.model.IpAnaParam;

public class IpApiClient extends CommonApiClient {
    public IpApiClient(String accessKey, String secretKey) {
        super(accessKey, secretKey);
    }

    public String getTargetIpAnalysis(IpAnaParam param) {
        String json = JSONUtil.toJsonStr(param);
        return HttpRequest.post(GATEWAY_HOST + "/api/interfaceInvoke/ip/analysis/target")
                .addHeaders(getHeadMap(json, accessKey, secretKey))
                .body(json)
                .execute().body();
    }
}
