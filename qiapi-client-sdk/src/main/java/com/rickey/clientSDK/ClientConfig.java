package com.rickey.clientSDK;

import com.rickey.clientSDK.client.PayClient;
import com.rickey.clientSDK.client.QiApiClient;
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
    public QiApiClient qiApiClient() {
        return new QiApiClient(accessKey, secretKey);
    }

    @Bean
    public PayClient payClient() {
        return new PayClient(accessKey, secretKey);
    }

}
