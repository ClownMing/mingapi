package com.yupi.project.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;


@SpringBootTest
class UserInterfaceInfoServiceTest {

    @Resource
    UserInterfaceInfoService userInterfaceInfoService;

    @Test
    void invokeCount() {
        boolean count = userInterfaceInfoService.invokeCount(1L, 1L);
        Assertions.assertTrue(count);
    }
}
