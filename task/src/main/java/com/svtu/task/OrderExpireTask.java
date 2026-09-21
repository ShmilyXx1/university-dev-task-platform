package com.svtu.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.svtu.entity.Order;
import com.svtu.mapper.OrderMapper;
import com.svtu.service.PayService;
import com.svtu.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 订单过期自动关闭定时任务。
 * 每分钟扫描"待接取(state=0)且已过截止时间(endDatetime<now)"的订单，
 * 对已付赏金/押金调支付宝原路退款，然后将订单置为已取消(state=3)。
 * 关单时复用接单分布式锁(order:lock:{orderId})，避免与用户接单并发冲突。
 */
@Slf4j
@Component
public class OrderExpireTask {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private PayService payService;
    @Autowired
    private RedisUtil redisUtil;

    @Scheduled(fixedRate = 60_000)
    public void closeExpiredOrders() {
        List<Order> expired = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getState, "0")
                        .lt(Order::getEndDatetime, new Date())
        );
        if (expired == null || expired.isEmpty()) return;
        log.info("订单过期扫描：发现 {} 笔待关闭订单", expired.size());

        for (Order order : expired) {
            int orderId = order.getOrderId();
            String lockKey = "order:lock:" + orderId;
            String lockVal = "scheduler";
            // 复用接单锁：抢不到说明用户正在接单，跳过本轮，下轮再处理
            if (!redisUtil.tryLock(lockKey, lockVal, 60)) continue;
            try {
                // 退款：refund 内部只改 t_payment 流水，不改 t_order，需自行更新订单状态
                String newPayState = order.getPayState();
                String newDepositState = order.getDepositState();
                if ("1".equals(order.getPayState())) {
                    if (payService.refund(orderId, "BOUNTY", "订单过期未接取，退还赏金")) {
                        newPayState = "3"; // 已退款
                    }
                }
                if ("1".equals(order.getDepositState())) {
                    if (payService.refund(orderId, "DEPOSIT", "订单过期未接取，退还押金")) {
                        newDepositState = "2"; // 已退还
                    }
                }
                // 条件更新：仅当仍是待接取(state=0)时才关闭，避免与接单竞态
                orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                        .eq(Order::getOrderId, orderId)
                        .eq(Order::getState, "0")
                        .set(Order::getState, "3")
                        .set(Order::getPayState, newPayState)
                        .set(Order::getDepositState, newDepositState));
                log.info("订单过期关闭成功：orderId={}", orderId);
            } catch (Exception e) {
                // 单条失败不阻塞其它订单；下轮扫描会重试（refund 找不到 state=1 流水时返回 false，天然幂等）
                log.error("订单过期关闭失败：orderId={}", orderId, e);
            } finally {
                redisUtil.unlock(lockKey, lockVal);
            }
        }
    }
}
