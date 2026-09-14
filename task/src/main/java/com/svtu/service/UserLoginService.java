package com.svtu.service;

import com.svtu.common.Result;
import com.svtu.entity.User;

public interface UserLoginService {
    Result<Object> AdminLogin(User user);
    Result<Object> UserLogin(User user);
}
