package com.svtu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 聊天消息表（支持客服聊天 + 用户间 P2P 聊天）
 */
@Data
@TableName("t_chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Integer messageId;
    private Integer fromUserId;
    private Integer toUserId;
    /** 消息类型: chat-普通聊天, service-客服匹配, system-系统通知 */
    private String msgType;
    private String content;
    /** 0=未读, 1=已读 */
    private Integer isRead;
    private Date sendDatetime;
}
