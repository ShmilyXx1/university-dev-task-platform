package com.svtu.controller;

import com.svtu.common.Result;
import com.svtu.service.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 支付接口（支付宝沙箱）
 */
@RestController
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private PayService payService;

    /**
     * 创建支付（返回收银台表单HTML）
     * POST /pay/create?orderId=15&type=BOUNTY
     * type: BOUNTY=赏金托管  DEPOSIT=押金
     */
    @PostMapping("/create")
    public Result<String> create(@RequestParam("orderId") int orderId,
                                 @RequestParam("type") String payType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId = (Integer) authentication.getPrincipal();
        return payService.createPay(orderId, payType, userId);
    }

    /**
     * 查询支付状态（主动向支付宝查询）
     * GET /pay/status?orderId=15&type=BOUNTY
     * 返回 data: "1"=已支付  "0"=未支付
     */
    @GetMapping("/status")
    public Result<String> status(@RequestParam("orderId") int orderId,
                                 @RequestParam("type") String payType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId = (Integer) authentication.getPrincipal();
        return payService.queryPay(orderId, payType, userId);
    }
}
