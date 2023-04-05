package com.ming.mingapiclientsdk;

import com.ming.mingapiclientsdk.client.MingApiClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author ClownMing
 * @version 1.0
 * @description TODO
 * @date 2023/3/29 21:39
 */
@Configuration
@ConfigurationProperties("mingapi-client")
@ComponentScan
@Data
public class MingApiClientConfig {

    private String accessKey;

    private String secretKey;

    @Bean
    public MingApiClient mingApiClient() {
        return new MingApiClient(accessKey, secretKey);
    }
}
