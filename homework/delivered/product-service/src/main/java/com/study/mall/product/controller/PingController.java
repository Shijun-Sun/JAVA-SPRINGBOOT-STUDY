package com.study.mall.product.controller;


import com.study.mall.product.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public Result<String> ping() {
       return Result.ok("pong");
    }

}
