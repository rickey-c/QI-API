package com.rickey.apiInterface.service.impl;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rickey.apiInterface.model.entity.City;
import com.rickey.apiInterface.service.CityService;
import com.rickey.apiInterface.service.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author adorabled4
 * @className WeatherServiceImpl
 * @date : 2023/04/15/ 17:19
 **/
@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    private final CityService cityService;

    private final String apiKey = "65d6e771f85980a7874acbb0098d61a3";
    ;
    public static final String DEFAULT_CITY = "北京";
    public static final int DEFAULT_CITY_CODE = 110000;
    public static final String API_URL = "https://restapi.amap.com/v3/weather/weatherInfo";

    public WeatherServiceImpl(CityService cityService) {
        this.cityService = cityService;
    }

    @Override
    public String getWeatherByCityName(String cityName) {
        String url = API_URL;
        Map<String, Object> params = new HashMap<>();
        params.put("key", apiKey);
        City city = cityService.query().eq("name", cityName).one();
        log.info("city = {}", city);

        if (city == null) {
            // city == null，则进行剪切然后再次比对查找
            String trimCityName = trimCityName(cityName);
            List<City> cities = cityService.query().like("name", trimCityName).list();
            if (cities.isEmpty()) {
                params.put("city", DEFAULT_CITY_CODE); // 查询北京
            } else {
                for (City tmpCity : cities) {
                    params.put("city", tmpCity.getAdCode());
                }
            }
        } else {
            params.put("city", city.getAdCode());
        }

        String response = HttpUtil.get(url, params);
        log.info("response = {}", response);

        // 调用处理方法解析天气信息
        return parseWeatherResponse(response);
    }


    /**
     * 去掉地区名的后缀 : 比如 郑州市 => 郑州
     *
     * @param cityName
     * @return
     */
    private String trimCityName(String cityName) {
        String[] suffixs = new String[]{"自治县", "自治区", "自治州", "市", "县", "区", "市辖区", "省"};
        for (String suffix : suffixs) {
            if (cityName.endsWith(suffix)) {
                int lastIndexOf = cityName.lastIndexOf(suffix);
                return cityName.substring(0, lastIndexOf);
            }
        }
        return cityName;
    }

    /**
     * 解析天气响应数据并返回格式化的天气信息
     *
     * @param response JSON响应字符串
     * @return 格式化后的天气信息
     */
    private String parseWeatherResponse(String response) {
        JSONObject jsonResponse = JSONObject.parseObject(response);
        if (jsonResponse.getInteger("status") == 1) {
            JSONArray lives = jsonResponse.getJSONArray("lives");
            if (lives != null && !lives.isEmpty()) {
                JSONObject weatherData = lives.getJSONObject(0);
                String province = weatherData.getString("province");
                String cityNameInResponse = weatherData.getString("city");
                String weather = weatherData.getString("weather");
                String temperature = weatherData.getString("temperature");
                String windDirection = weatherData.getString("winddirection");
                String windPower = weatherData.getString("windpower");
                String humidity = weatherData.getString("humidity");
                String reportTime = weatherData.getString("reporttime");

                // 格式化并返回用户友好的天气信息
                return String.format(
                        "天气报告（%s %s）：\n" +
                                "天气：%s\n" +
                                "温度：%s°C\n" +
                                "风向：%s\n" +
                                "风力：%s\n" +
                                "湿度：%s%%\n" +
                                "更新时间：%s\n",
                        province, cityNameInResponse, weather, temperature,
                        windDirection, windPower, humidity, reportTime
                );
            }
        }

        return "无法获取天气信息，请稍后重试。";
    }
}