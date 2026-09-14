package com.svtu.controller;

import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.exception.UserException;
import com.svtu.service.UserForgetPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserForgetPasswordController {
    @Autowired
    private UserForgetPasswordService userForgetPasswordService;
    @Autowired
    private Common common;

    //1.获取验证码
    @GetMapping("/forgetPasswordGetCode")
    public Result<Object> getCode(String phone){
        //登录、注册、忘记密码校验大致相同，就放到一个类中进行数据校验
        common.checkPhone(phone);
        return userForgetPasswordService.getCode(phone);
    }

    //2.验证码匹配是否成功
    @GetMapping("/forgetPasswordCheckCode")
    public Result<Object> checkCode(String phone, String checkCode){
        if(checkCode == null){
            throw new UserException(501,"验证码为空");
        }
        return userForgetPasswordService.checkCode(phone,checkCode);
    }

    //3.更新密码
    @PutMapping("/updatePassword")
    public Result<Object> updatePassword(String phone, String newPassword, String againPassword){
        if(phone == null || phone.trim().isEmpty()){
            throw new UserException(501,"非法操作，不可直接更新");
        }
        common.checkPassword(newPassword, againPassword);
        return userForgetPasswordService.updatePassword(phone,newPassword);
    }
}
