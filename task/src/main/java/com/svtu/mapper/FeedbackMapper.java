package com.svtu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.svtu.VO.FeedbackVO;
import com.svtu.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
    List<FeedbackVO> selectAllByUserIdSolveFeedback(@Param("solve") String solve, @Param("userId") int userId);
    Integer updateUserFeedbackByContent(@Param("content")String content,@Param("feedbackId") int feedbackId);
    Integer updateUserFeedbackBySolve(@Param("solve") String solve,@Param("feedbackId") int feedbackId);
    Integer serviceReply(@Param("feedbackId") int feedbackId, @Param("reply") String reply);
    List<FeedbackVO> selectAllFeedback(String type);
}
