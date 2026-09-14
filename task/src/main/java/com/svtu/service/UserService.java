package com.svtu.service;

import com.svtu.VO.UserVO;
import com.svtu.common.Result;
import com.svtu.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    Result<UserVO> selectUser(int userId);
    Result<UserVO> selectProfileUser(String username);
    Result<UserVO> updateUser(User user);
    Result<Void> updatePhone(String phone,int userId);

}
