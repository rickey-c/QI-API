package com.rickey.clientSDK.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.rickey.common.model.dto.request.WeatherParam;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WeatherApiClient extends CommonApiClient {
    public WeatherApiClient(String accessKey, String secretKey) {
        super(accessKey, secretKey);
    }


    public String getCurrentWeatherByCity(WeatherParam weatherParam) {
        String json = JSONUtil.toJsonStr(weatherParam);
        return HttpRequest.post(GATEWAY_HOST + "/api/interfaceInvoke/weather/now")
                .addHeaders(getHeadMap(json, accessKey, secretKey))
                .body(json)
                .execute().body();
    }

}
