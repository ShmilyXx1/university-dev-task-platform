package com.svtu.service;

import com.svtu.common.Result;
import com.svtu.entity.Order;

import java.util.List;

public interface AuditorService {
    Result<Void> auditorUpdateOrder(int orderId, String auditorComplete);//管理员审核,管理员端接口
    Result<List<Order>> selectAuditorAllUserCompleteOrder();//后端查询所有userComplete为1接口
    Result<Order> selectAuditorOneOrder(int orderId);
}
