package com.rickey.clientSDK.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.rickey.clientSDK.model.IpAnaParam;

/**
 * IP API 客户端，用于调用与 IP 相关的分析接口。
 */
public class IpApiClient extends CommonApiClient {

    /**
     * 构造函数，用于初始化 `IpApiClient` 实例。
     *
     * @param accessKey 开发者的访问密钥
     * @param secretKey 开发者的安全密钥
     */
    public IpApiClient(String accessKey, String secretKey) {
        super(accessKey, secretKey);
    }

    /**
     * 获取指定目标 IP 的分析结果。
     *
     * @param param 包含目标 IP 分析参数的对象
     * @return 分析结果的 JSON 字符串
     */
    public String getTargetIpAnalysis(IpAnaParam param) {
        String json = JSONUtil.toJsonStr(param);
        return HttpRequest.post(GATEWAY_HOST + "/api/interfaceInvoke/ip/analysis/target")
                .addHeaders(getHeadMap(json, accessKey, secretKey))
                .body(json)
                .execute().body();
    }
}
