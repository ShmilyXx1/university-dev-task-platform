package com.svtu.mapper;

import com.svtu.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuditorMapper {
    Integer auditorUpdateOrder(@Param("orderId") int orderId, @Param("auditorComplete") String auditorComplete);
    List<Order> selectAllUserCompleteOrder();
    Order selectUserCompleteOneOrder(@Param("orderId") int orderId);
}
