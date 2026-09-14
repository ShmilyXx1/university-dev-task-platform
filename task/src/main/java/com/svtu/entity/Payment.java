package com.svtu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付流水实体（每一笔钱都有记录）
 */
@Data
@TableName("t_payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long paymentId;
    private Integer orderId;          // 订单ID
    private Integer userId;           // 付款用户ID
    private String payType;           // BOUNTY=赏金托管  DEPOSIT=押金
    private BigDecimal amount;        // 支付金额
    private String outTradeNo;        // 商户订单号（自己生成，全局唯一）
    private String tradeNo;           // 支付宝流水号
    private String state;             // 0待支付 1支付成功 2已关闭
    private Date createTime;
    private Date payTime;
}
