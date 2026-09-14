package com.svtu.service;

import com.sun.org.apache.xpath.internal.operations.Or;
import com.svtu.VO.OrderVO;
import com.svtu.common.Result;
import com.svtu.entity.Order;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
public interface OrderService {
    Result<List<OrderVO>> selectSearchOrder(String searchNum);
    Result<List<OrderVO>> selectAllOrder();
    /** 客服/管理员：查询全部订单（所有状态），可按状态/关键字筛选 */
    Result<List<OrderVO>> selectAllOrderForService(String state, String keyword);
    Result<List<OrderVO>> selectFilter(String type, BigDecimal minPrice,BigDecimal maxPrice,String sort);
    Result<OrderVO> selectOneOrder(int orderId);
    Result<Integer> insertOrder(Order order, int userId);
    Result<Void> getterUpdateOrder(int orderId,int userId);//考虑并发情况,他人接单时
    Result<Void> senderUpdateOrder(Order order,int userId);//自己修改订单时
    Result<Void> resultUpdateOrder(int orderId,String documentPath);//接单人提交交付地址
    Result<Void> deleteOrder(int orderId,int userId);
    Result<List<OrderVO>> selectSenderOrderByState(String state,int userId);
    Result<List<OrderVO>> selectGetterOrderByState(String state,int userId);
    Result<Void> CancelGetterUpdateOrder(int orderId);
    Result<Void> revokeCompleteOrder(int orderId, int userId);//接单人撤销"已完成"，回退为已被接取
}
