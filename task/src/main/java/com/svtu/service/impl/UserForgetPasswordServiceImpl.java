package com.svtu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.svtu.common.Result;
import com.svtu.entity.User;
import com.svtu.exception.UserException;
import com.svtu.mapper.UserForgetPasswordMapper;
import com.svtu.service.UserForgetPasswordService;
import com.svtu.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Random;

@Service
@Transactional
public class UserForgetPasswordServiceImpl implements UserForgetPasswordService {

    @Autowired
    private UserForgetPasswordMapper userForgetPasswordMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;

    //1.给手机号发送验证码
    @Override
    public Result<Object> getCode(String phone) {
        //查询用户是否存在
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone,phone);
        User user=userForgetPasswordMapper.selectOne(queryWrapper);
        if(Objects.isNull(user)){
            throw new UserException(501,"该手机号还未注册");
        }
        //判断验证码获取是否成功
        String code=Code();
        if(code == null){
            throw new UserException(501,"验证码获取失败，请重新获取");
        }
        //存入redis中，时间为1分钟过期
        redisUtil.set(phone,code,60);
        System.out.println(code);
        return Result.success(code);
    }

    //2.验证码是否匹配
    @Override
    public Result<Object> checkCode(String phone, String checkCode) {
        String code=(String)redisUtil.get(phone);
        if(code == null){
            throw new UserException(500,"验证码已过期或者没有获取验证码");
        }
        if(!code.equals(checkCode)){
            throw new UserException(500,"验证码错误");
        }
        return Result.success(phone);
    }

    //3.验证通过更新密码
    @Override
    public Result<Object> updatePassword(String phone, String password) {
        LambdaQueryWrapper<User> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone,phone);
        User user=userForgetPasswordMapper.selectOne(wrapper);
        if(passwordEncoder.matches(password,user.getPassword())){
            throw new UserException(500,"不能和旧密码相同");
        }
        LambdaUpdateWrapper<User> updateWrapper=new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getPhone,phone)
                .set(User::getPassword,passwordEncoder.encode(password));
        if(userForgetPasswordMapper.update(null,updateWrapper) <= 0) {
            throw new UserException(500,"更新密码失败");
        }
        return Result.success();
    }

    //获取一个4位数的验证码
    public String Code(){
        Random random = new Random();
        // 生成 0 ~ 9999 的数字，然后格式化为 4 位，前面自动补 0
        return String.format("%04d", random.nextInt(10000));
    }
}
