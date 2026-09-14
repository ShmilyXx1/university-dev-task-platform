package com.svtu.service;

import com.svtu.common.Result;
import com.svtu.entity.User;

import java.util.List;

public interface AdminUserService {
    Result<List<User>> AllUser();
    Result<User> AddUser(User user);
    Result<Void> UpdateUserState(int userId,String state);
    Result<Void> AddUserRole(int userId,int roleId);
    Result<List<String>> getAllUserRole(int userId);
    Result<Void> deleteUserRole(int userId,int roleId);
}
