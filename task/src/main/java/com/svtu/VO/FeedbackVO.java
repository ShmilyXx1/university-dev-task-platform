package com.svtu.VO;

import com.svtu.entity.Feedback;
import lombok.Data;

import java.util.Date;
@Data
public class FeedbackVO {
    private int feedbackId;
    private int userId;
    private String username;
    private String content;
    private String reply;//客服回复内容
    private String solve;//是否解决0未解决,1解决
    private Date sendDatetime;
    private Date replyDatetime;
    private Date solveDatetime;
    public FeedbackVO(){

    }
    public FeedbackVO(Feedback feedback){
       this.feedbackId=feedback.getFeedbackId();
       this.userId=feedback.getUserId();
       this.content=feedback.getContent();
       this.reply=feedback.getReply();
       this.solve=feedback.getSolve();
       this.sendDatetime=feedback.getSendDatetime();
       this.replyDatetime=feedback.getReplyDatetime();
       this.solveDatetime=feedback.getSolveDatetime();
    }
}
