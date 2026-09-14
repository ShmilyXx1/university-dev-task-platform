package com.svtu.service.impl;

import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.entity.Role;
import com.svtu.entity.User;
import com.svtu.entity.UserRole;
import com.svtu.exception.AdminException;
import com.svtu.mapper.*;
import com.svtu.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
@Transactional
@Service
public class AdminUserServiceImpl implements AdminUserService {
    @Autowired
    private AdminUserMapper adminUserMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private UserRegisterMapper userRegisterMapper;
    @Autowired
    private Common common;

    @Override
    public Result<List<User>> AllUser() {
        List<User>  list=adminUserMapper.selectAllUser();
        if (list==null||list.isEmpty()){
           list=new ArrayList<>();
        }
        return Result.success(list);
    }

    @Override
    public Result<User> AddUser(User user) {
        int rows = adminUserMapper.insert(user);
        if (rows<=0){
            throw new AdminException(502,"添加用户失败");
        }
        User user1=userRegisterMapper.selectByPhoneUser(user.getPhone());
        UserRole userRole=new UserRole();
        userRole.setUserId(user1.getUserId());
        userRole.setRoleId(4);//默认为普通用户
        int i = userRoleMapper.insert(userRole);
        if (i<=0){
            throw new AdminException(502,"用户身份添加失败");
        }
        return Result.success(user1);
    }


    @Override
    public Result<Void> UpdateUserState(int userId, String state) {
        common.checkUserId(userId);
        int rows = adminUserMapper.updateUserState(userId, state);
        if (rows<=0){
            throw new AdminException(502,"管理员修改用户状态失败");
        }
        return Result.success();
    }

    @Override
    public Result<Void> AddUserRole(int userId,int roleId) {
        common.checkUserId(userId);
        if (roleId<=0||roleId>=5){
            throw new AdminException(502,"角色名错误");
        }
        UserRole userRole=new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        int rows = userRoleMapper.insert(userRole);
        if (rows<=0){
            throw new AdminException(502,"给用户添加角色失败");
        }
        return Result.success();
    }

    @Override
    public Result<List<String>> getAllUserRole(int userId) {
        common.checkUserId(userId);
        List<String> list= userRoleMapper.selectUserRoleName(userId);
        if (list==null||list.isEmpty()){
            list=new ArrayList<>();
        }
        return Result.success(list);
    }

    @Override
    public Result<Void> deleteUserRole(int userId, int roleId) {
        common.checkUserId(userId);
        if (roleId<=0||roleId>=5){
            throw new AdminException(502,"角色名为空");
        }
        UserRole userRole=new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        int rows = userRoleMapper.deleteById(userRole);
        if (rows<=0){
            throw new AdminException(502,"删除用户角色失败");
        }
        return Result.success();
    }

}

