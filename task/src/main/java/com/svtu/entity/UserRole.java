package com.svtu.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_user_role")
public class UserRole {
    @TableId
    private int userId;
    private int roleId;
}
