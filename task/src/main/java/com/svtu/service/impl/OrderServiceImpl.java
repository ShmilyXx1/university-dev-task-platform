package com.svtu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.svtu.VO.OrderVO;
import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.entity.Order;
import com.svtu.exception.UserException;
import com.svtu.mapper.OrderMapper;
import com.svtu.service.OrderService;
import com.svtu.service.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private Common common;
    @Autowired
    private PayService payService;

    /**
     * 查询所有未被接单的订单
     */
    @Override
    public Result<List<OrderVO>> selectAllOrder(){
        List<OrderVO> list = orderMapper.selectAllOrder();
        if (list==null||list.isEmpty()) {
            list=new ArrayList<>();
        }
        return Result.success(list);
    }

    @Override
    // 客服/管理员：查询全部订单（所有状态），空串归一化为 null
    public Result<List<OrderVO>> selectAllOrderForService(String state, String keyword){
        if ("".equals(state) || "all".equals(state)) state = null;
        if (keyword != null && keyword.trim().isEmpty()) keyword = null;
        List<OrderVO> list = orderMapper.selectAllOrderForService(state, keyword);
        if (list==null||list.isEmpty()) {
            list=new ArrayList<>();
        }
        return Result.success(list);
    }

    /**
     * 模糊查询订单
     *
     * @param searchNum
     * @return
     */
    @Override
    public Result<List<OrderVO>> selectSearchOrder(String searchNum){
        if (searchNum==null){
            throw new UserException(501,"查询条件为空");
        }
        List<OrderVO> list = orderMapper.selectOrder(searchNum);
        if (list.isEmpty()||list==null){
            list=new ArrayList<>();
        }
        return Result.success(list);

    }

    /**
     * 筛选
     * @param type
     * @param minPrice
     * @param maxPrice
     * @param sort
     * @return
     */
    @Override
    public Result<List<OrderVO>> selectFilter(String type, BigDecimal minPrice, BigDecimal maxPrice,String sort) {
        List<OrderVO> list = orderMapper.selectFilter(type, minPrice, maxPrice,sort);
        if (list.isEmpty()||list==null){
            list=new ArrayList<>();
        }
        return Result.success(list);
    }


    /*修改订单这块有四种情况:(state四种状态0,1,2,3,分别代表待被接取,已被接取,完成订单,接单人取消)
     * 1.当他人接单时(需修改订单整体状态为1且接单id)
     * 2.自己修改订单内容时(可修改订单标题、内容、类型、订单价格、订单押金)
     * 3.他人完成订单时(需管理员审核为1然后修改订单整体状态为2,最终时间)
     * 4.接单人取消订单时(订单整体状态为3,还需将交付路径取消,userComplete为0,审核员为0)*/
    /**
     *发起接单,更改订单状态
     * @param orderId
     * @param userId
     * @return
     */
    @Override
    @Transactional //考虑并发情况,他人接单时
    public Result<Void> getterUpdateOrder(int orderId, int userId) {
        common.checkOrderId(orderId);
        common.checkUserId(userId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new UserException(501, "订单不存在");
        }
        if (!"1".equals(order.getPayState())) {
            throw new UserException(501, "发单人尚未托管赏金，无法接单");
        }
        if (!"1".equals(order.getDepositState())) {
            throw new UserException(501, "请先支付押金");
        }
        int rows=orderMapper.getterUpdateOrder(orderId,userId);
        if(rows==1){
            return Result.success();
        }else {
            throw new UserException(501,"该订单已被接取");
        }

    }

    @Override//发单人自行修改订单内容，只允许修改自己发布的订单
    public Result<Void> senderUpdateOrder(Order order,int userId) {
        if (order==null){
            throw new UserException(501,"修改订单内容是空的");
        }
        common.checkOrderId(order.getOrderId());
        Order exist = orderMapper.selectById(order.getOrderId());
        if (exist == null) {
            throw new UserException(501, "订单不存在");
        }
        if (exist.getSenderId() != userId) {
            throw new UserException(501, "无权修改该订单，只能修改自己发布的订单");
        }
        int rows=orderMapper.senderUpdateOrder(order,userId);
        if (rows<=0){
            throw new UserException(501,"订单修改失败");
        }
        return Result.success();
    }

    @Override//提交结果路径
    public Result<Void> resultUpdateOrder(int orderId, String documentPath) {
        common.checkOrderId(orderId);
        if (documentPath==null){
            throw new UserException(501,"交付路径为空");
        }
        int rows=orderMapper.resultUpdateOrder(orderId,documentPath);
        if (rows<=0){
            throw new UserException(501,"交付路径修改失败");
        }
        return Result.success();
    }
    /**
     * 某个订单的详细
     * @param orderId
     * @return
     */
    @Override
    public Result<OrderVO> selectOneOrder(int orderId) {
        common.checkOrderId(orderId);
        OrderVO orderVO = orderMapper.selectOneOrder(orderId);
        return Result.success(orderVO);
    }

    @Override
    public Result<Integer> insertOrder(Order order,int userId) {
        if (order==null){
            throw new UserException(501,"新增订单是空的");
        }
        common.checkUserId(userId);
        int rows=0;
        if (order.getSenderId()==0){
        order.setSenderId(userId);
        }
        rows = orderMapper.insert(order);
        if (rows<=0){
            throw new UserException(501,"订单添加失败");
        }
        return Result.success(order.getOrderId());
    }
    @Override
    @Transactional
    public Result<Void> deleteOrder(int orderId,int userId) {
        common.checkOrderId(orderId);
        common.checkUserId(userId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new UserException(501, "订单不存在");
        }
        // 权限：发单人本人或拥有"管理员"角色的用户
        boolean admin = common.isAdmin(userId);
        if (order.getSenderId() != userId && !admin) {
            throw new UserException(501, "无权取消该订单");
        }

        if (admin) {
            // 管理员：物理删除（数据清理），删除前先退款
            if ("1".equals(order.getPayState())) {
                payService.refund(orderId, PayServiceImpl.TYPE_BOUNTY, "管理员删除订单，赏金退还");
            }
            if ("1".equals(order.getDepositState())) {
                payService.refund(orderId, PayServiceImpl.TYPE_DEPOSIT, "管理员删除订单，押金退还");
            }
            int rows = orderMapper.deleteOrder(orderId, userId);
            if (rows <= 0) {
                throw new UserException(501, "订单删除失败");
            }
            return Result.success();
        }

        // 普通发单人：仅待被接取(state=0)可取消，标记为已取消(state=3)，记录保留
        if (!"0".equals(order.getState())) {
            throw new UserException(501, "订单已被接取或已结束，无法取消；如需终止请联系接单人取消接单");
        }
        if ("1".equals(order.getPayState())) {
            // 赏金已托管 -> 原路退还给发单人
            payService.refund(orderId, PayServiceImpl.TYPE_BOUNTY, "发单人取消订单，赏金退还");
        }
        int rows = orderMapper.cancelOrder(orderId, userId);
        if (rows <= 0) {
            throw new UserException(501, "订单取消失败");
        }
        return Result.success();
    }

    @Override
    public Result<List<OrderVO>> selectSenderOrderByState(String state, int userId) {
        return getListResult(state, userId, true);
    }

    @Override
    public Result<List<OrderVO>> selectGetterOrderByState(String state, int userId) {
        return getListResult(state, userId, false);
    }

    private Result<List<OrderVO>> getListResult(String state, int userId, Boolean t) {
        if (state != null) {
            state = state.trim();
            if ("".equals(state) || "all".equalsIgnoreCase(state)) {
                state = null;
            }
        }

        if (state != null && !"0".equals(state) && !"1".equals(state)
                && !"2".equals(state) && !"3".equals(state)) {
            throw new UserException(501, "订单状态编号错误");
        }

        List<OrderVO> list = null;
        if (Boolean.TRUE.equals(t)) {
            list = orderMapper.selectSenderUserOrderByState(state, userId);
        } else {
            list = orderMapper.selectGetterUserOrderByState(state, userId);
        }

        if (list == null || list.isEmpty()) {
            list = new ArrayList<>();
        }
        return Result.success(list);
    }

    @Override
    public Result<Void> CancelGetterUpdateOrder(int orderId) {
        if (orderId<=0){
            return Result.error("订单编号错误");
        }
        Integer rows = orderMapper.CancelGetterUpdateOrder(orderId);
        if (rows<=0){
            return Result.error("用户取消订单失败");
        }
        // 取消接单成功后：若押金已支付，原路退还给接单人，并重置押金状态
        // （订单回到待接取池，下一位接单人需重新支付押金）
        Order order = orderMapper.selectById(orderId);
        if (order != null && "1".equals(order.getDepositState())) {
            payService.refund(orderId, PayServiceImpl.TYPE_DEPOSIT, "接单人取消接单，押金原路退还");
            order.setDepositState("0");
            order.setDepositTradeNo(null);
            orderMapper.updateById(order);
        }
        return Result.success();
    }

    @Override
    public Result<Void> revokeCompleteOrder(int orderId, int userId) {
        common.checkOrderId(orderId);
        common.checkUserId(userId);
        int rows = orderMapper.revokeCompleteOrder(orderId, userId);
        if (rows <= 0) {
            throw new UserException(501, "撤销失败：订单不是已完成状态，或您不是该订单的接单人");
        }
        return Result.success();
    }

}
