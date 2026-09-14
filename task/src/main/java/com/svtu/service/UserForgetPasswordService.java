package com.svtu.service;

import com.svtu.common.Result;

public interface UserForgetPasswordService {
    Result<Object> getCode(String phone);
    Result<Object> checkCode(String phone,String code);
    Result<Object> updatePassword(String phone, String password);
}
