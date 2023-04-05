package com.ming.mingapiclientsdk.client;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.ming.mingapiclientsdk.model.User;

import java.util.HashMap;
import java.util.Map;

import static com.ming.mingapiclientsdk.utils.SignUtils.genSign;


/**
 * @author ClownMing
 * @version 1.0
 * @description 调用第三方接口的客户端
 * @date 2023/3/29 19:00
 */
public class MingApiClient {

    private String accessKey;
    private String secretKey;

    public MingApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }



    private Map<String, String> getHeaderMap(String body) {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("accessKey", accessKey);
        // 一定不能发送给后端(不能放到请求头中)
//        hashMap.put("secretKey", secretKey);
        // 随机数
        hashMap.put("nonce", RandomUtil.randomNumbers(4));
        // 用户请求参数
        hashMap.put("body", body);
        // 时间戳
        hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        // 通过 用户参数 + 秘钥 => 签名生成算法 => 不可解密的值
        hashMap.put("sign", genSign(body, secretKey));
        return hashMap;
    }


    public String getNameByGet(String name) {
        //可以单独传入http参数，这样参数会自动做URL编码，拼接在URL中
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", "ming");
        String result = HttpUtil.get("http://localhost:8090/api/name/", paramMap);
        System.out.println(result);
        return result;
    }

    public String getNameByPost(String name) {
        //可以单独传入http参数，这样参数会自动做URL编码，拼接在URL中
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", "ming");
        String result = HttpUtil.post("http://localhost:8090/api/name/", paramMap);
        System.out.println(result);
        return result;

    }

    public String getUserNameByPost(User user) {
        String json = JSONUtil.toJsonStr(user);
        HttpResponse httpResponse = HttpRequest.post("http://localhost:8090/api/name/json")
                .addHeaders(getHeaderMap(json))
                .body(json)
                .execute();
        String result = httpResponse.body();
        System.out.println(result + "状态码：" + httpResponse.getStatus());
        return result;

    }


}
