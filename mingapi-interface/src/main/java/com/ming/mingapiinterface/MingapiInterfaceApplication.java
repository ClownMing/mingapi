package com.ming.mingapiinterface;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ming.mingapiinterface.mapper")
public class MingapiInterfaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MingapiInterfaceApplication.class, args);
    }

}
