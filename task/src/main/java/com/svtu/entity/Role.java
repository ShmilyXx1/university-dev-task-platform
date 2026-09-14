package com.svtu.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_role")
public class Role {
    @TableId
    private int roleId;
    private String roleName;
}
