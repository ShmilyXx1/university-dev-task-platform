package com.svtu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.svtu.VO.OrderVO;
import com.svtu.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    List<OrderVO> selectAllOrder();
    /** 客服/管理员：查询全部订单（所有状态），可按状态/关键字筛选 */
    List<OrderVO> selectAllOrderForService(@Param("state") String state, @Param("keyword") String keyword);
    List<OrderVO> selectOrder(String searchNum);
    List<OrderVO> selectFilter(@Param("type") String type, @Param("minPrice") BigDecimal minPrice,@Param("maxPrice") BigDecimal maxPrice,@Param("sort") String sort);

    OrderVO selectOneOrder(int orderId);
    Integer getterUpdateOrder(@Param("orderId") int orderId, @Param("userId") int userId);
    Integer CancelGetterUpdateOrder(@Param("orderId") int orderId);
    Integer revokeCompleteOrder(@Param("orderId") int orderId, @Param("userId") int userId);
    Integer senderUpdateOrder(@Param("order") Order order,@Param("userId") int userId);
    Integer resultUpdateOrder(@Param("orderId") int orderId,@Param("documentPath") String documentPath);
    List<OrderVO> selectSenderUserOrderByState(@Param("state") String state,@Param("userId") int userId);
    List<OrderVO> selectGetterUserOrderByState(@Param("state") String state,@Param("userId") int userId);
    Integer deleteOrder(@Param("orderId") int orderId,@Param("userId") int userId);
    /** 发单人取消订单：仅待被接取(state=0)、仅本人，标记为已取消(state=3)，记录保留 */
    Integer cancelOrder(@Param("orderId") int orderId,@Param("userId") int userId);

}
