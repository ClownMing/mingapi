package com.ming.mingapiclientsdk.utils;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;

/**
 * @author ClownMing
 * @version 1.0
 * @description 签名工具
 * @date 2023/3/29 21:01
 */
public class SignUtils {
    /**
     * 生成签名
     */
    public static String genSign(String body, String secretKey) {
        Digester md5 = new Digester(DigestAlgorithm.SHA256);
        String content = body + "." + secretKey;
        String digestHex = md5.digestHex(content);
        return digestHex;
    }

}
