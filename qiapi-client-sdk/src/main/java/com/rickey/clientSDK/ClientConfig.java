package com.rickey.clientSDK;

import com.rickey.clientSDK.client.NameApiClient;
import com.rickey.clientSDK.client.RandomApiClient;
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

}
