package com.svtu.service;

import com.svtu.VO.FeedbackVO;
import com.svtu.common.Result;
import com.svtu.entity.Feedback;

import java.util.List;

public interface FeedbackService {
    Result<Void> addFeedback(String content,int userId);
    Result<FeedbackVO> getFeedback(int feedbackId);
    Result<List<FeedbackVO>> getUserAllFeedbackBySolve(String solve,int userId);
    Result<Void> updateUserFeedbackToContent(String content,int feedbackId);
    Result<Void> updateUserFeedbackToSolve(String solve,int feedbackId);
    Result<Void> deleteFeedback(int feedbackId);
    Result<Void> serviceReply(int feedbackId, String reply);
    Result<List<Feedback>> selectAllFeedback(String type);
}
