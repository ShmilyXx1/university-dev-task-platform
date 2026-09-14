package com.svtu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Integer messageId;
    private Integer fromUserId;
    private Integer toUserId;
    private String msgType;
    private String content;
    private Integer isRead;
    private Date sendDatetime;
}
