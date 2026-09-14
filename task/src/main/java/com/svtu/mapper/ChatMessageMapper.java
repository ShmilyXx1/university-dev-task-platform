package com.svtu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.svtu.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /** 查询两人之间的聊天历史（双向），SQL见 ChatMessageMapper.xml */
    List<ChatMessage> selectHistory(@Param("me") int me, @Param("peer") int peer);

    /** 查询某用户的未读消息（按发送者分组统计） */
    List<Map<String, Object>> selectUnreadCountByFrom(@Param("userId") int userId);

    /** 将某人发来的消息标记为已读 */
    int markAsRead(@Param("userId") int userId, @Param("fromUserId") int fromUserId);

    /** 将所有未读标记为已读 */
    int markAllAsRead(@Param("userId") int userId);
}
