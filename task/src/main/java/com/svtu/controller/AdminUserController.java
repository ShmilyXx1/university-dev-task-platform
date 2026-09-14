package com.svtu.controller;

import com.svtu.VO.UserVO;
import com.svtu.common.Result;
import com.svtu.entity.User;
import com.svtu.service.AdminUserService;
import com.svtu.service.AdminUserService;
import com.svtu.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/admin/user")
@PreAuthorize("hasRole('管理员')")
public class AdminUserController {
    @Autowired
    private AdminUserService adminUserService;
    @Autowired
    private FileService fileService;
    @GetMapping("/AllUser")//xx
    public Result<List<User>> selectAllUser(){
        return adminUserService.AllUser();
    }
    @PostMapping("/addUser")//xx
    public Result<User> insertUser(User user, @RequestParam(value = "avatar",required = false)MultipartFile avatar) throws IOException {
        if (avatar != null && !avatar.isEmpty()) {
            String imagePath = fileService.upload(avatar);
            user.setImagePath(imagePath);
        } else {
            // 没传头像，可以设置默认头像，或者不设置（数据库为null）
            user.setImagePath("/avatar/default-avatar.jpg");
        }
        return adminUserService.AddUser(user);
    }
    @PutMapping("/updateUserState")//xx
    public Result<Void> updateUserState(@RequestParam("userId") int userId,@RequestParam("state") String state){
        return adminUserService.UpdateUserState(userId,state);
    }
    @PutMapping("/addUserRole")//xx
    public Result<Void> updateUserPosition(@RequestParam("userId") int userId,@RequestParam("roleId") int roleId){
        return adminUserService.AddUserRole(userId,roleId);
    }
    @GetMapping("/getAllUserRole")//xx
    public Result<List<String>> getAllUserRole(@RequestParam("userId") int userId){
        return adminUserService.getAllUserRole(userId);
    }
    @DeleteMapping("/deleteUserRole")//xx
    public Result<Void> deleteUserRole(@RequestParam("userId") int userId,@RequestParam("roleId") int roleId){
        return adminUserService.deleteUserRole(userId,roleId);
    }
}
