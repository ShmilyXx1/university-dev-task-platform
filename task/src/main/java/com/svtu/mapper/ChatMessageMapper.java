package com.svtu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.svtu.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /** 查询两人之间的聊天历史（双向） */
    @Select("SELECT * FROM t_chat_message WHERE (from_user_id = #{me} AND to_user_id = #{peer}) " +
            "OR (from_user_id = #{peer} AND to_user_id = #{me}) ORDER BY send_datetime ASC LIMIT 200")
    List<ChatMessage> selectHistory(@Param("me") int me, @Param("peer") int peer);

    /** 查询某用户的未读消息（按发送者分组统计） */
    @Select("SELECT from_user_id AS fromUserId, COUNT(*) AS cnt FROM t_chat_message " +
            "WHERE to_user_id = #{userId} AND is_read = 0 GROUP BY from_user_id")
    List<Map<String, Object>> selectUnreadCountByFrom(@Param("userId") int userId);

    /** 将某人发来的消息标记为已读 */
    @Update("UPDATE t_chat_message SET is_read = 1 WHERE to_user_id = #{userId} AND from_user_id = #{fromUserId}")
    int markAsRead(@Param("userId") int userId, @Param("fromUserId") int fromUserId);

    /** 将所有未读标记为已读 */
    @Update("UPDATE t_chat_message SET is_read = 1 WHERE to_user_id = #{userId}")
    int markAllAsRead(@Param("userId") int userId);
}
