package com.rickey.clientSDK.utils;

import cn.hutool.core.util.RandomUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 获取请求头工具
 */
public class HeaderUtils {
    /**
     * 需要把body作为请求头传入的情况
     *
     * @return
     */
    public static Map<String, String> getHeaderMap(String accessKey, String secretKey, String body) {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("accessKey", accessKey);
        hashMap.put("nonce", RandomUtil.randomNumbers(4));
        hashMap.put("body", body);
        hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        hashMap.put("sign", SignUtils.genSign(body, secretKey));
        return hashMap;
    }

    /**
     * 需要把body作为请求头传入的情况
     *
     * @param accessKey
     * @param secretKey
     * @return
     */
    public static Map<String, String> getHeaderMap(String accessKey, String secretKey) {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("accessKey", accessKey);
        hashMap.put("nonce", RandomUtil.randomNumbers(4));
        hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        hashMap.put("sign", SignUtils.genSign(secretKey));
        return hashMap;
    }
}
