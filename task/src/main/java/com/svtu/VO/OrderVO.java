package com.svtu.VO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class OrderVO {
    private int orderId;
    private String senderName;
    private String getterName;
    private String title;
    private String content;
    private String type;
    private BigDecimal deposit;
    private BigDecimal senderPrice;
    private String state; //订单整体状态
    private String userComplete;//用户（接单人）是否完成任务
    private String auditorComplete;//审核员是否确认完成
    private Date releaseDatetime;//发布
    private Date endDatetime;//结束
    private Date completeDatetime;//最终
    private Date updateDatetime;
}
