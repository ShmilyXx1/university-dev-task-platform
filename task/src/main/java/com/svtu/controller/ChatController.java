package com.svtu.controller;

import com.svtu.common.Result;
import com.svtu.entity.ChatMessage;
import com.svtu.entity.User;
import com.svtu.mapper.ChatMessageMapper;
import com.svtu.mapper.UserMapper;
import com.svtu.util.RedisUtil;
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

    @Autowired
    private RedisUtil redisUtil;

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

    /**
     * 查询未读消息统计（按发送者分组）。
     * 读 Redis Hash（unread:{me} → field=发送者Id → value=未读数），O(1)；
     * Redis 丢失（重启后）则降级查 DB 并回填，保证自愈。
     */
    @GetMapping("/unread")
    public Result<Map<String, Object>> unread() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        int me = (Integer) auth.getPrincipal();
        String key = "unread:" + me;

        Map<String, String> cached = redisUtil.hGetAll(key);
        int total = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        if (cached.isEmpty()) {
            // 降级：Redis 无数据（冷启/重启丢失），查 DB 并回填 Redis
            List<Map<String, Object>> groups = chatMessageMapper.selectUnreadCountByFrom(me);
            for (Map<String, Object> row : groups) {
                int fromUserId = ((Number) row.get("fromUserId")).intValue();
                int cnt = ((Number) row.get("cnt")).intValue();
                redisUtil.hSet(key, String.valueOf(fromUserId), String.valueOf(cnt));
                if (cnt <= 0) continue;
                total += cnt;
                details.add(buildDetail(fromUserId, cnt));
            }
        } else {
            for (Map.Entry<String, String> e : cached.entrySet()) {
                int cnt;
                try {
                    cnt = Integer.parseInt(e.getValue());
                } catch (NumberFormatException ex) {
                    continue;
                }
                if (cnt <= 0) continue; // 已读会话跳过
                total += cnt;
                int fromUserId = Integer.parseInt(e.getKey());
                details.add(buildDetail(fromUserId, cnt));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUnread", total);
        result.put("details", details);
        return Result.success(result);
    }

    private Map<String, Object> buildDetail(int fromUserId, int cnt) {
        User u = userMapper.selectById(fromUserId);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fromUserId", fromUserId);
        item.put("username", u != null ? u.getUsername() : "用户" + fromUserId);
        item.put("unreadCount", cnt);
        return item;
    }

    /**
     * 标记消息已读。DB 与 Redis 双写：DB 更新 is_read，Redis Hash 置 0
     * （保留 field 便于后续新消息继续累加）。
     */
    @PutMapping("/read")
    public Result<Void> markRead(@RequestParam(value = "fromUserId", required = false) Integer fromUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        int me = (Integer) auth.getPrincipal();
        String key = "unread:" + me;
        if (fromUserId != null) {
            chatMessageMapper.markAsRead(me, fromUserId);
            redisUtil.hSet(key, String.valueOf(fromUserId), "0");
        } else {
            chatMessageMapper.markAllAsRead(me);
            redisUtil.delete(key);
        }
        return Result.success();
    }
}
