package com.svtu.controller;

import com.svtu.VO.FeedbackVO;
import com.svtu.common.Result;
import com.svtu.entity.Common;
import com.svtu.entity.Feedback;
import com.svtu.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Feedback")
public class FeedbackController {
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private Common common;

    @PostMapping("/addFeedback")
    public Result<Void> addFeedback(@RequestParam("content") String content){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId=(Integer) authentication.getPrincipal();
        common.checkUserId(userId);
        return feedbackService.addFeedback(content,userId);
    }
    @GetMapping("/getFeedback")
    public Result<FeedbackVO> getFeedback(@RequestParam("feedbackId") int feedbackId){
        return feedbackService.getFeedback(feedbackId);
    }
    @GetMapping("getUserAllFeedback")
    public Result<List<FeedbackVO>> AllUserFeedback(@RequestParam( value = "solve",required = false) String solve){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId=(Integer) authentication.getPrincipal();
        common.checkUserId(userId);
        return feedbackService.getUserAllFeedbackBySolve(solve,userId);
    }
    @PutMapping("/updateUserFeedbackByContent")
    public Result<Void> UpdateFeedback(@RequestParam("content") String content,@RequestParam("feedbackId") int feedbackId){
        return feedbackService.updateUserFeedbackToContent(content, feedbackId);
    }
    @PutMapping("/updateUserFeedbackBySolve")
    public Result<Void> updateUserFeedbackBySolveTime(@RequestParam("solve") String solve,@RequestParam("feedbackId") int feedbackId){
        return feedbackService.updateUserFeedbackToSolve(solve,feedbackId);
    }
    @DeleteMapping("/deleteUserFeedback")
    public Result<Void> deleteFeedback(@RequestParam("feedbackId") int feedbackId){
        return feedbackService.deleteFeedback(feedbackId);
    }
    @GetMapping("/AllFeedback")
    @PreAuthorize("hasRole('客服') or hasRole('管理员')")
    public Result<List<Feedback>> selectAllFeedback(@RequestParam(value = "type", required = false) String type){
        return feedbackService.selectAllFeedback(type);
    }
    @PutMapping("/serviceReply")
    @PreAuthorize("hasRole('客服') or hasRole('管理员')")
    public Result<Void> serviceReply(@RequestParam("feedbackId") int feedbackId,
                                     @RequestParam("reply") String reply){
        return feedbackService.serviceReply(feedbackId, reply);
    }

}
