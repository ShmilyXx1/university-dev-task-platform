package com.svtu.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_feedback")
public class Feedback {
    @TableId
    private int feedbackId;
    private int userId;
    private String content;
    private String solve;//是否解决0未解决,1解决
    private Date sendDatetime;
    private Date solveDatetime;
    private String reply;
    private Date replyDatetime;
    // JOIN t_user 得到的用户名，非表字段
    @TableField(exist = false)
    private String username;
}
