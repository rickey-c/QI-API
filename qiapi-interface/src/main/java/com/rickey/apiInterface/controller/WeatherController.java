package com.rickey.apiInterface.controller;

import com.rickey.apiInterface.service.WeatherService;
import com.rickey.common.model.dto.request.WeatherParam;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @PostMapping("/now")
    @Operation(summary = "天气查询API")
    public String getCurrentWeatherByCity(@RequestBody WeatherParam weatherParam) {
        // 获取城市名称
        String cityName = weatherParam.getCityName();

        // 检查城市名称是否为空，若为空抛出异常
        if (cityName == null || cityName.trim().isEmpty()) {
            log.error("City name is missing in the request: {}", weatherParam);
            throw new IllegalArgumentException("City name is required.");
        }

        log.info("weatherParam = {}", weatherParam);
        // 调用天气服务获取天气信息
        return weatherService.getWeatherByCityName(cityName);
    }

}
