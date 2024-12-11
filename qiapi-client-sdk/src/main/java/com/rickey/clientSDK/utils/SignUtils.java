package com.rickey.clientSDK.utils;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;

/**
 * 签名工具类，用于生成数据的签名。
 */
public class SignUtils {

    /**
     * 根据提供的请求体和密钥生成 SHA-256 签名。
     *
     * @param body     请求体内容（可以为空，为空时默认使用空字符串）
     * @param secretKey 密钥，用于生成签名
     * @return 生成的 SHA-256 签名字符串
     */
    public static String genSign(String body, String secretKey) {
        if (body == null) {
            body = "";
        }
        Digester md5 = new Digester(DigestAlgorithm.SHA256);
        String content = body + "." + secretKey;
        return md5.digestHex(content);
    }
}

