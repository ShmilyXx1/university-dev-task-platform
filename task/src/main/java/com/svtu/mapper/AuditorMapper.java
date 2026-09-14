package com.svtu.mapper;

import com.svtu.VO.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuditorMapper {
    Integer auditorUpdateOrder(@Param("orderId") int orderId, @Param("auditorComplete") String auditorComplete);
    List<OrderVO> selectAllUserCompleteOrder();
    OrderVO selectUserCompleteOneOrder(@Param("orderId") int orderId);
}
