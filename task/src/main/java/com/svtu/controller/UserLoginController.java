package com.svtu.controller;

import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.entity.User;
import com.svtu.exception.UserException;
import com.svtu.service.UserLoginService;
import com.svtu.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/user")
public class UserLoginController {

    @Autowired
    private UserLoginService userLoginService;
    @Autowired
    private Common common;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/userLogin")//前台登录接口
    public Result<Object> login(@RequestBody User user){
        if (Objects.isNull(user)) {
            throw new UserException(501,"手机号和密码不能为空");
        }

        common.checkLogin(user);
        return userLoginService.UserLogin(user);
    }
    @PostMapping("/adminLogin")//后台登录接口
    public Result<Object> adminLogin(@RequestBody User user){
        if (Objects.isNull(user)) {
            throw new UserException(501,"手机号和密码不能为空");
        }

        common.checkLogin(user);
        return userLoginService.AdminLogin(user);
    }

    @PostMapping("/logout")//退出登录：删除服务端会话缓存，令牌立即失效
    public Result<Object> logout(@RequestHeader(value = "token", required = false) String token){
        if (token == null || token.trim().isEmpty()) {
            throw new UserException(501,"未登录");
        }
        Claims claims = jwtUtil.extractAllClaims(token);
        Integer userId = (Integer) claims.get("userId");
        userLoginService.logout(userId);
        return Result.success();
    }
}
