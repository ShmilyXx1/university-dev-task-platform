package com.svtu.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_user")
public class User {
    @TableId
    private int userId;
    private String username;
    private String password;
    private String nickname;
    private  String phone;
    private String sex;
    private int age;
    private String email;
    private String address;
    private String imagePath;
    private Date registerDatetime;
    private Date updateDatetime;
    private String state;
}
