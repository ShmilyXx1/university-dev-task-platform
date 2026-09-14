package com.svtu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.svtu.VO.OrderVO;
import com.svtu.common.Result;
import com.svtu.entity.Order;
import com.svtu.exception.UserException;
import com.svtu.service.OrderService;
import com.svtu.service.impl.OrderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/AllOrder")
    public Result<List<OrderVO>> selectAllOrder() {
        return orderService.selectAllOrder();
    }

    /**
     * 客服/管理员：查询全部订单（所有状态），可按状态/关键字筛选
     * GET /order/AllOrderForService?state=0|1|2|3&keyword=xxx
     */
    @GetMapping("/AllOrderForService")
    @PreAuthorize("hasRole('客服') or hasRole('管理员')")
    public Result<List<OrderVO>> selectAllOrderForService(
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return orderService.selectAllOrderForService(state, keyword);
    }

    @GetMapping("/searchOrder")
    public Result<List<OrderVO>> selectSearchOrder(@RequestParam(required = false) String searchNum) {
        return orderService.selectSearchOrder(searchNum);

    }

    @GetMapping("/Filter")
    public Result<List<OrderVO>> selectFilter(@RequestParam(value = "type", required = false) String type,
                                              @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
                                              @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
                                              @RequestParam(value = "sort", required = false) String sort) {
        return orderService.selectFilter(type, minPrice, maxPrice, sort);
    }

    @PutMapping("/getterUpdateOrder")
    public Result<Void> getterUpdateOrder(@RequestParam("orderId") int orderId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId=(Integer) authentication.getPrincipal();
        return orderService.getterUpdateOrder(orderId,userId);
    }
    @PutMapping("/CancelGetterUpdateOrder")//只有接这个订单的用户才有资格取消订单,或者通过联系客服来取消,xx
    public Result<Void> CancelGetterUpdateOrder(@RequestParam("orderId") int orderId){
        return orderService.CancelGetterUpdateOrder(orderId);
    }

    /**
     * 接单人撤销"已完成"：已完成(state=2)回退为已被接取(state=1)，
     * 接单人需重新提交结果路径，订单再次进入审核流程
     */
    @PutMapping("/revokeComplete")
    public Result<Void> revokeComplete(@RequestParam("orderId") int orderId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId=(Integer) authentication.getPrincipal();
        return orderService.revokeCompleteOrder(orderId, userId);
    }
    @PutMapping("/senderUpdateOrder")
    public Result<Void> senderUpdateOrder(@RequestBody Order order){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId=(Integer) authentication.getPrincipal();
        return orderService.senderUpdateOrder(order,userId);
    }

    @PutMapping("/resultUpdateOrder")
    public Result<Void> resultUpdateOrder(@RequestParam("orderId") int orderId,@RequestParam(value = "documentPath",required = false) String documentPath){
        return orderService.resultUpdateOrder(orderId,documentPath);
    }

    @DeleteMapping("/deleteOrder")
    public Result<Void> deleteOrder(@RequestParam("orderId") int orderId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId=(Integer) authentication.getPrincipal();
        return orderService.deleteOrder(orderId,userId);
    }

    @PostMapping("/addOrder")
    public Result<Integer> insertOrder(@RequestBody Order order) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId=(Integer) authentication.getPrincipal();
        return orderService.insertOrder(order, userId);
    }
    @GetMapping("/OneOrder")
    public Result<OrderVO> selectOneOrder(@RequestParam("orderId") int orderId) {
        return orderService.selectOneOrder(orderId);
    }
    @GetMapping("/getAllSenderUserOrderByState")
    public Result<List<OrderVO>> getSenderAllUserOrderByState(@RequestParam(value = "state",required = false) String state){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId=(Integer) authentication.getPrincipal();
        return orderService.selectSenderOrderByState(state,userId);
    }
    @GetMapping("/getAllGetterUserOrderByState")
    public Result<List<OrderVO>> getGetterAllUserOrderByState(@RequestParam(value = "state",required = false) String state){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId=(Integer) authentication.getPrincipal();
        return orderService.selectGetterOrderByState(state,userId);
    }
}
