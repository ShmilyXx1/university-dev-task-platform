package com.svtu.service.impl;

import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.entity.Order;
import com.svtu.exception.AdminException;
import com.svtu.mapper.AuditorMapper;
import com.svtu.mapper.OrderMapper;
import com.svtu.service.AuditorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditorServiceImpl implements AuditorService {
    @Autowired
    private AuditorMapper auditorMapper;
    @Autowired
    private Common common;
    @Override//审核员审核,审核端接口(2为不通过,1为通过,默认为0带审核)
    public Result<Void> auditorUpdateOrder(int orderId, String auditorComplete) {
        common.checkOrderId(orderId);
        if(auditorComplete==null){
            throw new AdminException(502,"审核员审核状态为空");
        }
        int rows=auditorMapper.auditorUpdateOrder(orderId,auditorComplete);
        if (rows <= 0){
            throw new AdminException(502,"管理员审核失败");
        }
        return Result.success();
    }

    @Override
    public Result<List<Order>> selectAuditorAllUserCompleteOrder() {
        List<Order> list=auditorMapper.selectAllUserCompleteOrder();
        if (list.isEmpty()||list==null){
            list=new ArrayList<>();
        }
        return Result.success(list);
    }

    @Override
    public Result<Order> selectAuditorOneOrder(int orderId) {
        common.checkOrderId(orderId);
        Order order=auditorMapper.selectUserCompleteOneOrder(orderId);
        return Result.success(order);
    }
}
