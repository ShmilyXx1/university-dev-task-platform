package com.svtu.VO;

import com.svtu.entity.User;
import lombok.Data;

import java.util.Date;

@Data
public class UserVO {
    private Integer userId;
    private String username;
    private String nickname;
    private  String phone;
    private String position;
    private String sex;
    private int age;
    private String email;
    private String address;
    private String imagePath;
    private Date registerDatetime;
    public UserVO (){

    }
    public UserVO(User user){
        this.userId=user.getUserId();
        this.username=user.getUsername();
        this.nickname=user.getNickname();
        this.phone=user.getPhone();
        this.sex=user.getSex();
        this.age=user.getAge();
        this.email=user.getEmail();
        this.address=user.getAddress();
        this.imagePath=user.getImagePath();
        this.registerDatetime=user.getRegisterDatetime();
    }
}
