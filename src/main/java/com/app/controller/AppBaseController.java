package com.app.controller;

import com.app.common.Result;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
public class AppBaseController {
    
    @RequestMapping("/test")
    public Result<String> test() {
        return Result.success("Android API 连接成功！");
    }
}
