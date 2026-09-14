package com.svtu.controller;

import com.svtu.VO.OrderVO;
import com.svtu.common.Result;
import com.svtu.service.AuditorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auditor")
@PreAuthorize("hasRole('审核员') or hasRole('管理员')")
public class AuditorController {
    @Autowired
    private AuditorService auditorService;

    @PutMapping("/auditorUpdateOrder")//审核端接口
    public Result<Void> auditorUpdateOrder(@RequestParam("orderId") int orderId, @RequestParam("auditorComplete")String auditorComplete){
        return auditorService.auditorUpdateOrder(orderId,auditorComplete);
    }

    @GetMapping("/selectAuditorAllUserCompleteOrder")//审核端接口
    public Result<List<OrderVO>> selectAuditorAllUserComplete(){
        return auditorService.selectAuditorAllUserCompleteOrder();
    }
    @GetMapping("/selectAuditorOneOrder")//审核端接口
    public Result<OrderVO> selectAuditorOneOrder(@RequestParam("orderId") int orderId){
        return auditorService.selectAuditorOneOrder(orderId);
    }
}
