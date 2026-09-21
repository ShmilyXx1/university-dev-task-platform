package com.svtu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.svtu.VO.UserVO;
import com.svtu.common.Result;
import com.svtu.entity.User;
import com.svtu.entity.UserRole;
import com.svtu.exception.UserException;
import com.svtu.mapper.UserForgetPasswordMapper;
import com.svtu.mapper.UserRegisterMapper;
import com.svtu.mapper.UserRoleMapper;
import com.svtu.service.UserRegisterService;
import com.svtu.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Random;

@Service
@Transactional
public class UserRegisterServiceImpl implements UserRegisterService {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UserRegisterMapper userRegisterMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserForgetPasswordMapper userForgetPasswordMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;


    //注册：获取验证码
    @Override
    public Result<Object> registerGetCode(String phone) {
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone,phone);
        User user=userForgetPasswordMapper.selectOne(queryWrapper);
        if(user!=null){
            throw new UserException(501,"该手机号已被注册");
        }
        //判断验证码获取是否成功
        String code=Code();
        if(code == null){
            throw new UserException(501,"验证码获取失败，请重新获取");
        }
        System.out.println(code);
            //存入redis中，时间为1分钟过期
            redisUtil.set(phone,code,60);

        return Result.success();
    }


    @Override
    public Result<Object> checkCode(String phone, String checkCode) {
        String code=(String)redisUtil.get(phone);
        if(code == null){
            throw new UserException(501,"验证码已过期或者没有获取验证码");
        }
        if(!code.equals(checkCode)){
            throw new UserException(501,"验证码错误");
        }
        return Result.success(phone);
    }

    @Override
    public Result<UserVO> userRegister(User user) {
        if(Objects.isNull(user)){
            throw new UserException(501,"用户信息为空");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if((userRegisterMapper.insert(user) <= 0)){
            throw new UserException(501,"注册失败");
        }
        User user1=userRegisterMapper.selectByPhoneUser(user.getPhone());  //注册页面的用户.默认都为普通用户(4)
        UserRole userRole=new UserRole();
        userRole.setUserId(user1.getUserId());
        userRole.setRoleId(4);
        int rows= userRoleMapper.insert(userRole);
        if (rows<=0){
            throw new UserException(501,"添加身份信息失败");
        }
         UserVO userVo=new UserVO(user1);
        return Result.success(userVo);
    }

    //获取一个4位数的验证码
    public String Code(){
        Random random = new Random();
        // 生成 0 ~ 9999 的数字，然后格式化为 4 位，前面自动补 0
        return String.format("%04d", random.nextInt(10000));
    }
}
