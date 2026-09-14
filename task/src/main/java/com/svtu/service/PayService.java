package com.svtu.service;

import com.svtu.common.Result;

/**
 * 支付服务（支付宝沙箱）
 */
public interface PayService {

    /**
     * 创建支付，返回支付宝收银台表单HTML（前端 document.write 后自动跳转收银台）
     * @param orderId 订单ID
     * @param payType BOUNTY=赏金托管  DEPOSIT=押金
     * @param userId  当前登录用户ID
     */
    Result<String> createPay(int orderId, String payType, int userId);

    /**
     * 主动查询支付状态（向支付宝查询，支付成功则更新订单和流水）
     * 返回 data: "1"=已支付成功  "0"=未支付/处理中
     */
    Result<String> queryPay(int orderId, String payType, int userId);

    /**
     * 全额退款（调支付宝 alipay.trade.refund，原路退回）
     * @param orderId 订单ID
     * @param payType BOUNTY=赏金  DEPOSIT=押金
     * @param reason  退款原因
     * @return true=退款成功；false=该笔没有支付成功的流水，无需退款
     */
    boolean refund(int orderId, String payType, String reason);

    /**
     * 审核通过后的资金结算：
     * 1. 押金已支付 -> 原路退还给接单人，deposit_state 置为 2（已退还）
     * 2. 赏金已托管 -> 账面结算给接单人，pay_state 置为 2（已结算），并写结算流水
     * 赏金未托管时抛出异常（无托管赏金无法完成结算）
     */
    void settleOnAuditPass(int orderId);
}
