package com.rickey.clientSDK;

import com.rickey.clientSDK.client.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * QiApi 客户端配置
 */
@Configuration
@ConfigurationProperties("client")
@Data
@ComponentScan
public class ClientConfig {

    private String accessKey;

    private String secretKey;

    @Bean
    public NameApiClient nameApiClient() {
        return new NameApiClient(accessKey, secretKey);
    }

    @Bean
    public RandomApiClient randomApiClient() {
        return new RandomApiClient(accessKey, secretKey);
    }

    @Bean
    public DayApiClient dayApiClient() {
        return new DayApiClient(accessKey, secretKey);
    }

    @Bean
    public IpApiClient ipApiClient() {
        return new IpApiClient(accessKey, secretKey);
    }

    @Bean
    public WeatherApiClient weatherApiClient() {
        return new WeatherApiClient(accessKey, secretKey);
    }


}
