package com.svtu.VO;

import com.svtu.entity.Feedback;
import lombok.Data;

import java.util.Date;
@Data
public class FeedbackVO {
    private int feedbackId;
    private String content;
    private String solve;//是否解决0未解决,1解决
    private Date sendDatetime;
    private Date solveDatetime;
    public FeedbackVO(){

    }
    public FeedbackVO(Feedback feedback){
       this.feedbackId=feedback.getFeedbackId();
       this.content=feedback.getContent();
       this.solve=feedback.getSolve();
       this.sendDatetime=feedback.getSendDatetime();
       this.solveDatetime=feedback.getSolveDatetime();
    }
}
