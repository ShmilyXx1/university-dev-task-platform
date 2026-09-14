package com.svtu.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.svtu.common.Result;
import com.svtu.config.AlipayConfig;
import com.svtu.entity.Order;
import com.svtu.entity.Payment;
import com.svtu.exception.UserException;
import com.svtu.mapper.OrderMapper;
import com.svtu.mapper.PaymentMapper;
import com.svtu.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
@Transactional
public class PayServiceImpl implements PayService {

    @Autowired
    private AlipayClient alipayClient;
    @Autowired
    private AlipayConfig alipayConfig;
    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private OrderMapper orderMapper;

    public static final String TYPE_BOUNTY = "BOUNTY";    // 赏金托管
    public static final String TYPE_DEPOSIT = "DEPOSIT";  // 押金

    @Override
    public Result<String> createPay(int orderId, String payType, int userId) {
        // 1. 查订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new UserException(501, "订单不存在");
        }

        BigDecimal amount;
        String subject;

        // 2. 按支付类型校验
        if (TYPE_BOUNTY.equals(payType)) {
            if (order.getSenderId() != userId) {
                throw new UserException(501, "只有发单人能托管赏金");
            }
            if ("1".equals(order.getPayState())) {
                throw new UserException(501, "赏金已托管，无需重复支付");
            }
            amount = order.getSenderPrice();
            subject = "任务赏金托管-订单" + orderId;
        } else if (TYPE_DEPOSIT.equals(payType)) {
            if (order.getDeposit() == null || order.getDeposit().compareTo(BigDecimal.ZERO) <= 0) {
                throw new UserException(501, "该订单无需支付押金");
            }
            if ("1".equals(order.getDepositState())) {
                throw new UserException(501, "押金已支付，无需重复支付");
            }
            if (!"0".equals(order.getState())) {
                throw new UserException(501, "订单已被接取，无法支付押金");
            }
            amount = order.getDeposit();
            subject = "接单押金-订单" + orderId;
        } else {
            throw new UserException(501, "支付类型错误");
        }

        // 3. 生成商户订单号并写流水（待支付）
        String outTradeNo = payType + "_" + orderId + "_" + userId + "_" + System.currentTimeMillis();
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setPayType(payType);
        payment.setAmount(amount);
        payment.setOutTradeNo(outTradeNo);
        payment.setState("0");
        payment.setCreateTime(new Date());
        paymentMapper.insert(payment);

        // 4. 调支付宝电脑网站支付（page.pay），返回收银台表单HTML
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            // 支付完成跳回前端结果页（带上订单ID和支付类型）
            request.setReturnUrl(alipayConfig.getReturnUrl() + "?orderId=" + orderId + "&type=" + payType);
            request.setNotifyUrl(alipayConfig.getNotifyUrl());

            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", outTradeNo);
            bizContent.put("total_amount", amount.toPlainString());
            bizContent.put("subject", subject);
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
            request.setBizContent(bizContent.toJSONString());

            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            // body 是一段自动提交的 HTML 表单，前端 document.write 即可跳转收银台
            return Result.success(response.getBody());
        } catch (Exception e) {
            log.error("创建支付宝支付失败", e);
            throw new UserException(501, "创建支付失败：" + e.getMessage());
        }
    }

    @Override
    public Result<String> queryPay(int orderId, String payType, int userId) {
        // 查该订单该类型最新一条流水
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getPayType, payType)
                        .orderByDesc(Payment::getPaymentId)
                        .last("limit 1")
        );
        if (payment == null) {
            return Result.success("0");
        }
        // 本地已成功
        if ("1".equals(payment.getState())) {
            return Result.success("1");
        }

        // 主动向支付宝查询真实状态
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", payment.getOutTradeNo());
            request.setBizContent(bizContent.toJSONString());

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            String tradeStatus = response.getTradeStatus();
            log.info("支付宝查询订单{}支付状态：{}", payment.getOutTradeNo(), tradeStatus);

            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 更新流水
                payment.setState("1");
                payment.setTradeNo(response.getTradeNo());
                payment.setPayTime(new Date());
                paymentMapper.updateById(payment);

                // 更新订单支付状态
                Order order = orderMapper.selectById(orderId);
                if (order != null) {
                    if (TYPE_BOUNTY.equals(payType)) {
                        order.setPayState("1");
                        order.setPayTradeNo(response.getTradeNo());
                    } else {
                        order.setDepositState("1");
                        order.setDepositTradeNo(response.getTradeNo());
                    }
                    orderMapper.updateById(order);
                }
                return Result.success("1");
            }
            return Result.success("0");
        } catch (Exception e) {
            log.error("查询支付宝支付状态失败", e);
            // 查询失败不改动状态，前端可继续轮询
            return Result.success("0");
        }
    }

    @Override
    public boolean refund(int orderId, String payType, String reason) {
        // 1. 找该订单该类型最新一笔"支付成功"的流水
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getPayType, payType)
                        .eq(Payment::getState, "1")
                        .orderByDesc(Payment::getPaymentId)
                        .last("limit 1")
        );
        if (payment == null) {
            // 没有付过款，无需退款
            return false;
        }

        // 2. 调支付宝全额退款接口 alipay.trade.refund（原路退回）
        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", payment.getOutTradeNo());
            bizContent.put("refund_amount", payment.getAmount().toPlainString());
            bizContent.put("refund_reason", reason);
            // 退款请求号，保证同一笔退款重复请求幂等
            bizContent.put("out_request_no", "RF_" + payment.getOutTradeNo());
            request.setBizContent(bizContent.toJSONString());

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                // 3. 流水标记为已退款（state=3）
                payment.setState("3");
                paymentMapper.updateById(payment);
                log.info("退款成功：orderId={}, type={}, amount={}, 支付宝流水={}",
                        orderId, payType, payment.getAmount(), response.getTradeNo());
                return true;
            } else {
                log.error("退款失败：orderId={}, code={}, msg={}, subMsg={}",
                        orderId, response.getCode(), response.getMsg(), response.getSubMsg());
                throw new UserException(501, "退款失败：" + response.getSubMsg());
            }
        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用支付宝退款异常", e);
            throw new UserException(501, "退款异常：" + e.getMessage());
        }
    }

    @Override
    public void settleOnAuditPass(int orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new UserException(501, "订单不存在，无法结算");
        }

        // 1. 押金已支付 -> 原路退还给接单人
        if ("1".equals(order.getDepositState())) {
            refund(orderId, TYPE_DEPOSIT, "订单审核通过，押金原路退还接单人");
            order.setDepositState("2");   // 2=已退还
            log.info("订单{}押金已退还接单人", orderId);
        }

        // 2. 赏金结算给接单人（账面结算：托管赏金从平台划转给接单人）
        if ("1".equals(order.getPayState())) {
            order.setPayState("2");       // 2=已结算

            // 写一条结算流水
            Payment settle = new Payment();
            settle.setOrderId(orderId);
            settle.setUserId(order.getGetterId());
            settle.setPayType("SETTLE");
            settle.setAmount(order.getSenderPrice());
            settle.setOutTradeNo("SETTLE_" + orderId + "_" + System.currentTimeMillis());
            settle.setTradeNo("BOOK_SETTLE");
            settle.setState("1");
            settle.setCreateTime(new Date());
            settle.setPayTime(new Date());
            paymentMapper.insert(settle);
            log.info("订单{}赏金 ¥{} 已结算给接单人(userId={})",
                    orderId, order.getSenderPrice(), order.getGetterId());
        } else {
            // 赏金未托管（pay_state=0）或已退款(3)，没有可结算的钱，阻止审核完成
            throw new UserException(501, "该订单赏金未托管，无法完成结算，请先联系发单人托管赏金");
        }

        orderMapper.updateById(order);
    }
}
