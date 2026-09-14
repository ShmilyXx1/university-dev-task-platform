package com.svtu.service.impl;

import com.svtu.VO.OrderVO;
import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.exception.AdminException;
import com.svtu.mapper.AuditorMapper;
import com.svtu.service.AuditorService;
import com.svtu.service.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditorServiceImpl implements AuditorService {
    @Autowired
    private AuditorMapper auditorMapper;
    @Autowired
    private Common common;
    @Autowired
    private PayService payService;

    @Override//审核员审核,审核端接口(2为驳回,1为通过,默认为0待审核)
    @Transactional
    public Result<Void> auditorUpdateOrder(int orderId, String auditorComplete) {
        common.checkOrderId(orderId);
        if(auditorComplete==null){
            throw new AdminException(502,"审核员审核状态为空");
        }
        auditorComplete = auditorComplete.trim();
        if (!"1".equals(auditorComplete) && !"2".equals(auditorComplete)) {
            throw new AdminException(502, "审核状态参数错误（1=通过 2=驳回）");
        }
        int rows=auditorMapper.auditorUpdateOrder(orderId,auditorComplete);
        if (rows <= 0){
            throw new AdminException(502,"审核失败：订单不存在、未提交完成或已审核");
        }

        // 审核通过 -> 资金结算：押金原路退还接单人 + 托管赏金结算给接单人
        // 结算失败（如赏金未托管）会抛异常，整个事务回滚，审核状态不生效
        if ("1".equals(auditorComplete)) {
            payService.settleOnAuditPass(orderId);
        }
        return Result.success();
    }

    @Override
    public Result<List<OrderVO>> selectAuditorAllUserCompleteOrder() {
        List<OrderVO> list=auditorMapper.selectAllUserCompleteOrder();
        if (list==null||list.isEmpty()){
            list=new ArrayList<>();
        }
        return Result.success(list);
    }

    @Override
    public Result<OrderVO> selectAuditorOneOrder(int orderId) {
        common.checkOrderId(orderId);
        OrderVO order=auditorMapper.selectUserCompleteOneOrder(orderId);
        return Result.success(order);
    }
}
