package com.svtu.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_menu")
public class Menu {
    @TableId
    private int menuId;
    private String menuAccount;
    private String menuName;
}
