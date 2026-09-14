//package com.svtu.consumer;
//
//import com.svtu.config.RabbitConfig;
//import com.svtu.websocket.ChatWebSocket;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//@Component
//public class UserQueueConsumer {
//    @Autowired
//    private ChatWebSocket chatWebSocket;
//
//    // 监听排队队列
//    @RabbitListener(queues = RabbitConfig.USER_QUEUE)
//    public void listenWaitUser(int userId){
//        // 拿到排队用户 → 执行自动匹配逻辑
//        System.out.println("从MQ拿到排队用户："+userId);
//
//        // 这里调用你原来的 autoMatch 逻辑
//        chatWebSocket.autoMatch(userId);
//    }
//}