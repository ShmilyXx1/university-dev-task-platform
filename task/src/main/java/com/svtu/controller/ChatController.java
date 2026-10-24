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

    /** 查询与某人的聊天历史（双向，msgType 可选过滤客服/私信） */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(@RequestParam("peerId") int peerId,
            @RequestParam(value = "msgType", required = false) String msgType) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        int me = (Integer) auth.getPrincipal();
        List<ChatMessage> list = chatMessageMapper.selectHistory(me, peerId, msgType);
        // SQL 已按时间倒序取最近 200 条，这里反转为时间升序展示
        Collections.reverse(list);
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
     * 查询未读消息统计（按发送者 + 消息类型分组）。
     * 以 DB 为准（含 msgType，供前端区分客服/私信并正确路由）；
     * 同时回填 Redis Hash 供实时推送 getTotalUnread 使用，并自愈 Redis 重启丢失。
     */
    @GetMapping("/unread")
    public Result<Map<String, Object>> unread() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        int me = (Integer) auth.getPrincipal();
        String key = "unread:" + me;

        List<Map<String, Object>> groups = chatMessageMapper.selectUnreadCountByFrom(me);
        int total = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (Map<String, Object> row : groups) {
            int fromUserId = ((Number) row.get("fromUserId")).intValue();
            int cnt = ((Number) row.get("cnt")).intValue();
            // 历史数据 msg_type 为 null 或默认 'chat' 时都按客服会话处理
            String msgType = row.get("msgType") == null ? "service" : String.valueOf(row.get("msgType"));
            if ("chat".equals(msgType)) msgType = "service";
            total += cnt;
            details.add(buildDetail(fromUserId, msgType, cnt));
        }

        // 回填 Redis（按发送者聚合总数，供实时推送使用）
        if (groups.isEmpty()) {
            redisUtil.delete(key);
        } else {
            Map<Integer, Integer> aggregated = new LinkedHashMap<>();
            for (Map<String, Object> row : groups) {
                int fromUserId = ((Number) row.get("fromUserId")).intValue();
                int cnt = ((Number) row.get("cnt")).intValue();
                aggregated.merge(fromUserId, cnt, Integer::sum);
            }
            for (Map.Entry<Integer, Integer> e : aggregated.entrySet()) {
                redisUtil.hSet(key, String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUnread", total);
        result.put("details", details);
        return Result.success(result);
    }

    private Map<String, Object> buildDetail(int fromUserId, String msgType, int cnt) {
        User u = userMapper.selectById(fromUserId);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fromUserId", fromUserId);
        item.put("username", u != null ? u.getUsername() : "用户" + fromUserId);
        item.put("msgType", msgType);
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
