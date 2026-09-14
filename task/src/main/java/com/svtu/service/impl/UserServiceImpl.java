package com.svtu.service.impl;

import com.baomidou.mybatisplus.extension.handlers.GsonTypeHandler;
import com.svtu.VO.UserVO;
import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.entity.User;
import com.svtu.exception.UserException;
import com.svtu.mapper.UserMapper;
import com.svtu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private Common common;

    @Override
    public Result<UserVO> selectUser(int userId) {
        common.checkUserId(userId);
        UserVO user=userMapper.selectByUserId(userId);
        return Result.success(user);
    }

    @Override
    public Result<UserVO> selectProfileUser(String username) {
        UserVO user = userMapper.selectByUsernameUser(username);
        if (user==null){
            throw new UserException(501,"没有找到当前用户");
        }

        return Result.success(user);
    }

    @Override
    public Result<UserVO> updateUser(User user) {
        int rows= userMapper.updateUser(user);
        if (rows<=0){
            throw new UserException(501,"用户信息更改失败");
        }
        UserVO user1=userMapper.selectByUserId(user.getUserId());
        return Result.success(user1);
    }

    @Override
    public Result<Void> updatePhone(String phone, int userId) {
        UserVO user=userMapper.selectByUserId(userId);
        if (phone.equals(user.getPhone())){
            throw new UserException(501,"手机号不能与原号码一致");
        }
        common.checkPhone(phone);//判断手机号格式是否正确
        int rows=userMapper.updatePhone(phone,userId);
        if (rows<=0){
            throw new UserException(501,"修改手机号失败");
        }
        return Result.success();
    }
}
