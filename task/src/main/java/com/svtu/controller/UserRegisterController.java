package com.svtu.controller;


import com.svtu.VO.UserVO;
import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.entity.User;
import com.svtu.exception.UserException;
import com.svtu.service.FileService;
import com.svtu.service.UserRegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/user")
public class UserRegisterController {

    @Autowired
    private UserRegisterService userRegisterService;;
    @Autowired
    private Common common;
    @Autowired
    private FileService fileService;

    //1.获取验证码
    @GetMapping("/registerGetCode")
    public Result<Object> getCode(String phone){
        //登录、注册、忘记密码校验大致相同，就放到一个类中进行数据校验

        common.checkPhone(phone);
        return userRegisterService.registerGetCode(phone);
    }

    //2.验证码匹配是否成功
    @GetMapping("/registerCheckCode")
    public Result<Object> checkCode(String phone, String checkCode){
        if(checkCode == null){
            throw new UserException(501,"验证码为空");
        }
        return userRegisterService.checkCode(phone,checkCode);
    }

    //3.注册
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody User user, @RequestParam(value = "avatar",required = false) MultipartFile avatar) throws IOException {
        System.out.println(user);
        if(user.getPhone() == null || user.getPhone().trim().isEmpty()){
            throw new UserException(501,"非法操作，不能直接注册");
        }
        if (avatar != null && !avatar.isEmpty()) {
            String imagePath = fileService.upload(avatar);
            user.setImagePath(imagePath);
        } else {
            // 没传头像，可以设置默认头像，或者不设置（数据库为null）
            user.setImagePath("/avatar/default-avatar.jpg");
        }
        //登录、注册、忘记密码校验大致相同，就放到一个类中进行数据校验
        common.checkRegister(user);
        return userRegisterService.userRegister(user);
    }
}
