//package com.svtu.config;
//
//import org.springframework.amqp.core.*;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class RabbitConfig {
//
//    // 1. 定义排队用户队列名
//    public static final String USER_QUEUE = "user_wait_queue";
//    public static final String USER_EXCHANGE = "user_exchange";
//
//    // 声明队列
//    @Bean
//    public Queue userQueue(){
//        return QueueBuilder.durable(USER_QUEUE).build();
//    }
//
//    // 声明交换机
//    @Bean
//    public DirectExchange userExchange(){
//        return ExchangeBuilder.directExchange(USER_EXCHANGE).durable(true).build();
//    }
//
//    // 绑定队列和交换机
//    @Bean
//    public Binding userBinding(Queue userQueue, DirectExchange userExchange){
//        return BindingBuilder.bind(userQueue).to(userExchange).with("user.key");
//    }
//}