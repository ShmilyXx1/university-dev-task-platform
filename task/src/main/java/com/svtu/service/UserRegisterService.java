package com.svtu.service;

import com.svtu.VO.UserVO;
import com.svtu.common.Result;
import com.svtu.entity.User;

public interface UserRegisterService {
    Result<Object> checkCode(String phone, String code);
    Result<Object> registerGetCode(String phone);
    Result<UserVO> userRegister(User user);
}
