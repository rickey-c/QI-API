package com.rickey.clientSDK.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.rickey.common.model.dto.request.WeatherParam;
import lombok.extern.slf4j.Slf4j;

/**
 * 天气 API 客户端，用于调用与天气信息相关的接口。
 */
@Slf4j
public class WeatherApiClient extends CommonApiClient {

    /**
     * 构造函数，用于初始化 `WeatherApiClient` 实例。
     *
     * @param accessKey 开发者的访问密钥
     * @param secretKey 开发者的安全密钥
     */
    public WeatherApiClient(String accessKey, String secretKey) {
        super(accessKey, secretKey);
    }

    /**
     * 获取指定城市的实时天气信息。
     *
     * @param weatherParam 包含天气查询参数的对象
     * @return 实时天气信息的 JSON 字符串
     */
    public String getCurrentWeatherByCity(WeatherParam weatherParam) {
        // 将参数对象转为 JSON 字符串
        String json = JSONUtil.toJsonStr(weatherParam);
        log.info("Requesting current weather with params: {}", json);
        return HttpRequest.post(GATEWAY_HOST + "/api/interfaceInvoke/weather/now")
                .addHeaders(getHeadMap(json, accessKey, secretKey))
                .body(json)
                .execute()
                .body();
    }
}
