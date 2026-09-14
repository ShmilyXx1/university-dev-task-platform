package com.svtu.service.impl;

import com.svtu.VO.FeedbackVO;
import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.entity.Feedback;
import com.svtu.exception.AdminException;
import com.svtu.exception.UserException;
import com.svtu.mapper.FeedbackMapper;
import com.svtu.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service//问题反馈有客服来看
public class FeedbackServiceImpl implements FeedbackService {
    @Autowired
    private FeedbackMapper feedbackMapper;
    @Autowired
    private Common common;

    @Override
    public Result<Void> addFeedback(String content,int userId) {
        common.checkUserId(userId);
        if (content==null){
            throw new UserException(501,"当前问题的内容是空的");
        }
        Feedback feedback=new Feedback();
        feedback.setContent(content);
        feedback.setUserId(userId);
        int rows=feedbackMapper.insert(feedback);
        if (rows<=0){
            throw new UserException(501,"问题反馈新建失败");
        }
        return Result.success();
    }

    @Override
    public Result<FeedbackVO> getFeedback(int feedbackId) {
        common.checkFeedbackId(feedbackId);
        Feedback feedback = feedbackMapper.selectById(feedbackId);
        FeedbackVO feedbackVO=new FeedbackVO(feedback);
        return Result.success(feedbackVO);
    }

    @Override
    public Result<List<FeedbackVO>> getUserAllFeedbackBySolve(String solve, int userId) {
        common.checkUserId(userId);
        if (solve!=null&&(!"0".equals(solve) &&! "1".equals(solve))){
            throw new UserException(501,"solve值有问题");
        }
        List<FeedbackVO> list = feedbackMapper.selectAllByUserIdSolveFeedback(solve, userId);
        if (list==null||list.isEmpty()){
            list=new ArrayList<>();
        }
        return Result.success(list);
    }

    @Override
    public Result<Void> updateUserFeedbackToContent(String content, int feedbackId) {
        common.checkFeedbackId(feedbackId);
        Integer rows = feedbackMapper.updateUserFeedbackByContent(content, feedbackId);
        if (rows<=0){
            throw new UserException(501,"修改问题反馈失败");
        }
        return Result.success();
    }

    @Override
    public Result<Void> updateUserFeedbackToSolve(String solve, int feedbackId) {
        common.checkFeedbackId(feedbackId);
        Integer rows = feedbackMapper.updateUserFeedbackBySolve(solve,feedbackId);
        if (rows<=0){
            throw new UserException(501,"修改问题反馈状态失败");
        }
        return Result.success();
    }

    @Override
    public Result<Void> deleteFeedback(int feedbackId) {
        common.checkFeedbackId(feedbackId);
        int rows=feedbackMapper.deleteById(feedbackId);
        if (rows<=0){
            throw new UserException(501,"删除问题反馈失败");
        }
        return Result.success();
    }

    @Override
    public Result<Void> serviceReply(int feedbackId, String reply) {
        common.checkFeedbackId(feedbackId);
        if (reply == null || reply.trim().isEmpty()) {
            throw new AdminException(502, "回复内容不能为空");
        }
        Integer rows = feedbackMapper.serviceReply(feedbackId, reply.trim());
        if (rows <= 0) {
            throw new AdminException(502, "回复失败，反馈不存在");
        }
        return Result.success();
    }

    @Override
    public Result<List<Feedback>> selectAllFeedback(String type) {
        if (!("0".equals(type)||"1".equals(type)||type==null)){
            throw new AdminException(502,"解决类型错误");
        }
        List<Feedback> list = feedbackMapper.selectAllFeedback(type);
        if (list==null||list.isEmpty()){
            list=new ArrayList<>();
        }
        return Result.success(list);
    }
}
