package com.ming.mingapiinterface.controller;

import com.ming.mingapiclientsdk.model.User;
import com.ming.mingapiclientsdk.utils.SignUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ClownMing
 * @version 1.0
 * @description 查询名称接口
 * @date 2023/3/29 17:56
 */
@RestController
@RequestMapping("/name")
public class NameController {

    @GetMapping("/get")
    public String getNameByGet(String name) {
        return "GET 你的名字是 " + name;
    }

    @PostMapping("/post")
    public String getNameByPost(@RequestParam String name) {
        return "POST 你的名字是 " + name;
    }

    @PostMapping("/json")
    public String getUserNameByPost(@RequestBody User user, HttpServletRequest request) {
        System.out.println("小飞棍来咯");
        return "POST 用户名字是 " + user.getUsername();
    }
}
