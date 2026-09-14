package com.svtu.controller;

import com.svtu.common.Result;
import com.svtu.entity.Order;
import com.svtu.service.AuditorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auditor")
public class AuditorController {
    @Autowired
    private AuditorService auditorService;

    @PutMapping("/auditorUpdateOrder")//审核端接口
    public Result<Void> auditorUpdateOrder(@RequestParam("orderId") int orderId, @RequestParam("auditorComplete")String auditorComplete){
        return auditorService.auditorUpdateOrder(orderId,auditorComplete);
    }

    @GetMapping("/selectAuditorAllUserCompleteOrder")//审核端接口
    public Result<List<Order>> selectAuditorAllUserComplete(){
        return auditorService.selectAuditorAllUserCompleteOrder();
    }
    @GetMapping("/selectAuditorOneOrder")//审核端接口
    public Result<Order> selectAuditorOneOrder(@RequestParam("orderId") int orderId){
        return auditorService.selectAuditorOneOrder(orderId);
    }
}
