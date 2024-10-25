package com.rickey.qiapiclientsdk;

import com.rickey.qiapiclientsdk.client.QiApiClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * QiApi 客户端配置
 */
@Configuration
@ConfigurationProperties("qiapi.client")
@Data
@ComponentScan
public class QiApiClientConfig {

    private String accessKey;

    private String secretKey;

    @Bean
    public QiApiClient qiApiClient() {
        return new QiApiClient(accessKey, secretKey);
    }

}
