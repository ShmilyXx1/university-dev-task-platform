package com.svtu.controller;

import com.svtu.VO.UserVO;
import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.entity.User;
import com.svtu.exception.UserException;
import com.svtu.mapper.UserMapper;
import com.svtu.mapper.UserRoleMapper;
import com.svtu.service.FileService;
import com.svtu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private Common common;
    @Autowired
    private FileService fileService;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private UserMapper userMapper;


    @GetMapping("/getUser")
    public Result<UserVO> selectUser(){
        Authentication authentication= SecurityContextHolder.getContext().getAuthentication();
        int userId=(Integer) authentication.getPrincipal();
        if (userId<=0){
            throw new UserException(501,"用户id为空");
        }
        return userService.selectUser(userId);
    }
    @GetMapping("/getProfileUser")
    public Result<UserVO> selectGetProfile(@RequestParam("username") String username){
        return userService.selectProfileUser(username);
    }
    @PutMapping("/updateUser")   //逻辑很好,但是觉得还能修改一下
    public Result<UserVO> updateUser(User user,
                                     @RequestParam(value = "avatar", required = false) MultipartFile avatar,
                                     @RequestParam(value = "adminUserId", required = false) Integer adminUserId) throws IOException {
        // 1. 处理头像
        if (avatar != null && !avatar.isEmpty()) {
            String imagePath = fileService.upload(avatar);
            if (imagePath == null || imagePath.isEmpty()) {
                throw new UserException(501, "头像上传失败,请稍后重试");
            }
            user.setImagePath(imagePath);
        }

        // 2. 取当前登录人
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId = (Integer) authentication.getPrincipal();
        if (userId <= 0) {
            throw new UserException(501, "用户id为空");
        }

        // 3. 决定实际要改谁
        User targetUser;
        if (adminUserId != null && adminUserId > 0) {
            // 只有管理员才能改别人
            List<String> roles = userRoleMapper.selectUserRoleName(userId);
            boolean isAdmin = roles != null && roles.stream().anyMatch(r -> r != null && r.contains("管理员"));
            if (!isAdmin) {
                throw new UserException(501, "没有权限修改他人信息");
            }
            common.checkUserId(adminUserId);
            targetUser =userMapper.selectById(adminUserId);
        } else {
            common.checkUserId(userId);
            targetUser = userMapper.selectById(userId);
        }
        if (targetUser == null) throw new UserException(501, "用户不存在");

        // 4. 只拷贝允许修改的字段(白名单)
        if (user.getUsername() != null) targetUser.setUsername(user.getUsername());
        if (user.getNickname() != null) targetUser.setNickname(user.getNickname());
        if (user.getSex()      != null) targetUser.setSex(user.getSex());
        if (user.getAge()      !=0) targetUser.setAge(user.getAge());
        if (user.getEmail()    != null) targetUser.setEmail(user.getEmail());
        if (user.getAddress()  != null) targetUser.setAddress(user.getAddress());
        if (user.getImagePath()!= null) targetUser.setImagePath(user.getImagePath());
        // phone/password/position/state/registerDatetime 绝对不让这里改

        return userService.updateUser(targetUser);
    }
    @PutMapping("/updatePhone")
    public Result<Void> updatePhone(@RequestParam("phone") String phone){
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        int userId=(Integer) authentication.getPrincipal();
        if (userId<=0){
            throw new UserException(501,"用户id为空");
        }
        common.checkPhone(phone);
        return userService.updatePhone(phone,userId);
    }


    //检查客户端和管理端接口是否有重复,重复部分能优化的优化一下
}
