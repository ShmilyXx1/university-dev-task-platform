package com.svtu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_feedback")
public class Feedback {
    @TableId(type = IdType.AUTO)
    private int feedbackId;
    private int userId;
    private String content;
    private String reply;//客服回复内容
    private String solve;//是否解决0未解决,1解决
    private Date sendDatetime;
    private Date replyDatetime;//客服回复时间
    private Date solveDatetime;
}
