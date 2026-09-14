package com.svtu.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_order")
public class Order {
    @TableId
private int orderId;
private int senderId;
private int getterId;
private String title;
private String content;
private String type;
private BigDecimal deposit;
private BigDecimal senderPrice;
private String state; //订单整体状态
private String userComplete;//用户（接单人）是否完成任务
private String  documentPath;//交付成果的文件存储路径
private String auditorComplete;//审核员是否确认完成
private Date releaseDatetime;//发布
private Date endDatetime;//结束
private Date completeDatetime;//最终
private Date updateDatetime;
}
