package com.svtu.service;

import com.svtu.VO.OrderVO;
import com.svtu.common.Result;

import java.util.List;

public interface AuditorService {
    Result<Void> auditorUpdateOrder(int orderId, String auditorComplete);//管理员审核,管理员端接口
    Result<List<OrderVO>> selectAuditorAllUserCompleteOrder();//后端查询所有userComplete为1接口
    Result<OrderVO> selectAuditorOneOrder(int orderId);
}
