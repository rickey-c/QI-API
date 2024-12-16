package com.rickey.clientSDK.client;

import cn.hutool.core.util.RandomUtil;
import com.rickey.clientSDK.utils.SignUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用 API 客户端，用于处理与 API 网关的交互。
 */
public class CommonApiClient {

    /**
     * 开发者的访问密钥，用于认证 API 调用。
     */
    protected final String accessKey;

    /**
     * 开发者的安全密钥，用于签名生成。
     */
    protected final String secretKey;

    /**
     * API 网关的主机地址。
     */
    protected static final String GATEWAY_HOST = "http://124.222.215.143:8090";

    /**
     * 构造函数，用于初始化 API 客户端实例。
     *
     * @param accessKey 开发者的访问密钥
     * @param secretKey 开发者的安全密钥
     */
    public CommonApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * 生成包含数字签名的请求头映射。
     *
     * @param body      请求体内容，用于签名计算
     * @param accessKey 开发者的访问密钥
     * @param secretKey 开发者的安全密钥，用于生成签名
     * @return 包含签名和其他必要信息的请求头映射
     */
    protected static Map<String, String> getHeadMap(String body, String accessKey, String secretKey) {
        // 创建请求头映射，包含六个参数
        Map<String, String> headMap = new HashMap<>();
        headMap.put("accessKey", accessKey); // 开发者的访问密钥
        headMap.put("body", body); // 请求体内容
        headMap.put("sign", SignUtils.genSign(body, secretKey)); // 生成的数字签名
        headMap.put("nonce", RandomUtil.randomNumbers(4)); // 随机数（4位数字）
        headMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000)); // 时间戳（秒级别）
        return headMap;
    }
}
