package com.rickey.clientSDK.client;

/**
 * 支付功能的客户端
 */
public class PayClient {

    private static final String GATEWAY_HOST = "http://localhost:8090";

    private String accessKey;

    private String secretKey;

    public PayClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }


}
