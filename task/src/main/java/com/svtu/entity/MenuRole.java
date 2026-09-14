package com.svtu.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_menu_role")
public class MenuRole {
    @TableId
    private int menuId;
    private int roleId;
}
