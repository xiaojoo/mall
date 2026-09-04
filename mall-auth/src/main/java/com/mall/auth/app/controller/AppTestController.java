package com.mall.auth.app.controller;


import com.mall.common.utils.Result;
import com.mall.auth.app.annotation.Login;
import com.mall.auth.app.annotation.LoginUser;
import com.mall.auth.app.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * APP测试接口
 *
 * @author mall
 */
@RestController
@RequestMapping("/app")
@Tag(name = "APP测试接口", description = "APP测试接口")
public class AppTestController {

    @Login
    @GetMapping("userInfo")
    
    public Result<Object> userInfo(@LoginUser UserEntity user){
        return Result.success(user);
    }

    @Login
    @GetMapping("userId")
    
    public Result<Object> userInfo(@RequestAttribute("userId") Integer userId){
        return Result.success(userId);
    }

    @GetMapping("notToken")
    
    public Result<Object> notToken(){
        return Result.success("无需token也能访问。。。");
    }

}
