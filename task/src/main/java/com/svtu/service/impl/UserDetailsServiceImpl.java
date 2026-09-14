package com.svtu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.svtu.entity.Menu;
import com.svtu.entity.User;
import com.svtu.entity.UserLogin;
import com.svtu.exception.UserException;
import com.svtu.mapper.AdminMenuMapper;
import com.svtu.mapper.UserLoginMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserLoginMapper userLoginMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        LambdaQueryWrapper<User> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone,phone);
        User user=userLoginMapper.selectOne(queryWrapper);
        if(Objects.isNull(user)){
           throw new UsernameNotFoundException("登陆失败,手机号或者密码错误");
        }
            if (user.getState().equals("1")) {  //判断账号是否被封禁或冻结
                throw new LockedException("账号已被封禁或冻结,请联系客服");
            }
        return new UserLogin(user);
    }
}
