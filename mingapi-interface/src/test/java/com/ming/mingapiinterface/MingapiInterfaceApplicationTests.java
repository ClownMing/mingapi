package com.ming.mingapiinterface;

import com.ming.mingapiclientsdk.client.MingApiClient;
import com.ming.mingapiclientsdk.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class MingapiInterfaceApplicationTests {

    @Resource
    private MingApiClient mingApiClient;

    @Test
    void contextLoads() {
        String result1 = mingApiClient.getNameByGet("ming");
        String result2 = mingApiClient.getNameByPost("ming");
        User user = new User();
        user.setUsername("clownMing");
        String result3 = mingApiClient.getUserNameByPost(user);
        System.out.println(result1);
        System.out.println(result2);
        System.out.println(result3);
    }

}
