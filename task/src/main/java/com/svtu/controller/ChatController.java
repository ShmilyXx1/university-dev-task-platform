package com.svtu.controller;

import com.svtu.common.Result;
import com.svtu.entity.ChatMessage;
import com.svtu.entity.User;
import com.svtu.mapper.ChatMessageMapper;
import com.svtu.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private UserMapper userMapper;

    /** 查询与某人的聊天历史（双向） */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(@RequestParam("peerId") int peerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        int me = (Integer) auth.getPrincipal();
        List<ChatMessage> list = chatMessageMapper.selectHistory(me, peerId);
        List<Map<String, Object>> res = new ArrayList<>();
        for (ChatMessage m : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("messageId", m.getMessageId());
            map.put("fromUserId", m.getFromUserId());
            map.put("toUserId", m.getToUserId());
            map.put("content", m.getContent());
            map.put("sendDatetime", m.getSendDatetime());
            map.put("isRead", m.getIsRead());
            res.add(map);
        }
        return Result.success(res);
    }

    /** 查询未读消息统计（按发送者分组） */
    @GetMapping("/unread")
    public Result<Map<String, Object>> unread() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        int me = (Integer) auth.getPrincipal();

        int total = 0;
        List<Map<String, Object>> groups = chatMessageMapper.selectUnreadCountByFrom(me);
        List<Map<String, Object>> details = new ArrayList<>();
        for (Map<String, Object> row : groups) {
            int fromUserId = ((Number) row.get("fromUserId")).intValue();
            int cnt = ((Number) row.get("cnt")).intValue();
            total += cnt;
            User u = userMapper.selectById(fromUserId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fromUserId", fromUserId);
            item.put("username", u != null ? u.getUsername() : "用户" + fromUserId);
            item.put("unreadCount", cnt);
            details.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUnread", total);
        result.put("details", details);
        return Result.success(result);
    }

    /** 标记消息已读 */
    @PutMapping("/read")
    public Result<Void> markRead(@RequestParam(value = "fromUserId", required = false) Integer fromUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        int me = (Integer) auth.getPrincipal();
        if (fromUserId != null) {
            chatMessageMapper.markAsRead(me, fromUserId);
        } else {
            chatMessageMapper.markAllAsRead(me);
        }
        return Result.success();
    }
}
