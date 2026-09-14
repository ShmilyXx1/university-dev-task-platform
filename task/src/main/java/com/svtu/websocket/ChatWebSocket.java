package com.svtu.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.svtu.entity.ChatMessage;
import com.svtu.entity.User;
import com.svtu.entity.UserLogin;
import com.svtu.mapper.ChatMessageMapper;
import com.svtu.mapper.UserMapper;
import com.svtu.mapper.UserRoleMapper;
import com.svtu.util.JwtUtil;
import com.svtu.util.RedisUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一聊天 WebSocket（客服聊天 + 用户 P2P 聊天 + 通知推）
 * 前端连接地址: ws://host/ws/chat/{token}
 */
@Slf4j
@Component
@ServerEndpoint("/ws/chat/{token}")
public class ChatWebSocket {

    // ===== 依赖注入（通过 static setter 在 Spring 启动后注入） =====
    private static JwtUtil jwtUtil;
    private static RedisUtil redisUtil;
    private static UserMapper userMapper;
    private static UserRoleMapper userRoleMapper;
    private static ChatMessageMapper chatMessageMapper;

    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) { ChatWebSocket.jwtUtil = jwtUtil; }
    @Autowired
    public void setRedisUtil(RedisUtil redisUtil) { ChatWebSocket.redisUtil = redisUtil; }
    @Autowired
    public void setUserMapper(UserMapper userMapper) { ChatWebSocket.userMapper = userMapper; }
    @Autowired
    public void setUserRoleMapper(UserRoleMapper userRoleMapper) { ChatWebSocket.userRoleMapper = userRoleMapper; }
    @Autowired
    public void setChatMessageMapper(ChatMessageMapper chatMessageMapper) { ChatWebSocket.chatMessageMapper = chatMessageMapper; }

    // ===== 全局状态（static，所有 WebSocket 实例共享） =====
    /** userId → Session（所有在线用户） */
    public static final Map<Integer, Session> allSessionMap = new ConcurrentHashMap<>();
    /** userId → 是否客服/管理员（可接单的角色） */
    public static final Map<Integer, Boolean> userIsServiceMap = new ConcurrentHashMap<>();
    /** 空闲客服 FIFO 队列 */
    public static final Queue<Integer> freeServiceQueue = new LinkedList<>();
    /** 排队用户（LinkedHashSet 保序） */
    public static final Set<Integer> waitingUsers = Collections.synchronizedSet(new LinkedHashSet<>());
    /** userId → serviceId（用户绑定的客服） */
    public static final Map<Integer, Integer> userBindService = new ConcurrentHashMap<>();
    /** serviceId → userId（客服绑定的用户） */
    public static final Map<Integer, Integer> serviceBindUser = new ConcurrentHashMap<>();
    /** 排队位置计数器（用于给用户展示"前方还有 N 人"） */
    public static final AtomicInteger queueSeq = new AtomicInteger(0);

    // ===== 当前会话属性（每个实例独有） =====
    private int myUserId = 0;
    private boolean amService = false;

    // ==================== 连接建立 ====================
    @OnOpen
    public void onOpen(@PathParam("token") String token, Session session) {
        try {
            if (jwtUtil == null) {
                log.warn("JwtUtil 未注入，WebSocket 连接被拒绝");
                closeQuietly(session, 1011, "server not ready");
                return;
            }
            // 1. 解析 token
            Claims claims = jwtUtil.extractAllClaims(token);
            Integer uid = (Integer) claims.get("userId");
            if (uid == null) {
                closeQuietly(session, 1008, "invalid token");
                return;
            }
            // 2. 校验 Redis 登录态
            UserLogin userLogin = (UserLogin) redisUtil.get("login:" + uid);
            if (userLogin == null) {
                closeQuietly(session, 1008, "not logged in");
                return;
            }
            User user = userLogin.getUser();
            this.myUserId = uid;
            // 3. 判断是否客服（旧副本 UserLogin 无 roles 字段，直接从角色表查询角色名）
            List<String> roles = userRoleMapper.selectUserRoleName(uid);
            this.amService = roles != null && (roles.contains("客服") || roles.contains("管理员"));
            // 4. 注册 session
            allSessionMap.put(uid, session);
            userIsServiceMap.put(uid, this.amService);
            // 5. 客服上线 → 加入空闲队列
            if (this.amService && !freeServiceQueue.contains(uid)) {
                freeServiceQueue.offer(uid);
            }
            log.info("[WS] 用户 {} 连接成功, isService={}, online={}", uid, this.amService, allSessionMap.size());

            // 6. 欢迎消息
            JSONObject welcome = new JSONObject();
            welcome.put("type", this.amService ? "service_online" : "user_online");
            welcome.put("content", "ok");
            sendMsg(session, welcome.toJSONString());

        } catch (Exception e) {
            log.error("[WS] onOpen 异常", e);
            closeQuietly(session, 1011, "internal error");
        }
    }

    // ==================== 收到消息 ====================
    @OnMessage
    public void onMessage(String message, Session session) {
        if (myUserId == 0) return; // 未认证
        try {
            JSONObject dto = JSON.parseObject(message);
            String type = dto.getString("type");
            if (type == null) return;

            switch (type) {
                case "user_apply":        handleUserApply(session); break;
                case "user_cancel":       handleUserCancel(); break;
                case "service_pull":      handleServicePull(); break;
                case "service_finish":    handleServiceFinish(); break;
                case "user_state_query":  handleStateQuery(session); break;
                case "chat":              handleServiceChat(dto); break;
                case "user_chat":         handleP2PChat(dto); break;
                case "typing":            handleTyping(dto); break;
                default:
                    log.warn("[WS] 未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("[WS] onMessage error", e);
        }
    }

    // ==================== 连接关闭 ====================
    @OnClose
    public void onClose(Session session) {
        if (myUserId == 0) return;
        log.info("[WS] 用户 {} 断开", myUserId);
        cleanupOnDisconnect();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.warn("[WS] onError userId={}: {}", myUserId, throwable.getMessage());
    }

    // ==================== 具体处理逻辑 ====================

    /** 用户申请排队 */
    private void handleUserApply(Session session) {
        if (amService) return; // 客服不能排队
        // 如果已经在会话中，不允许再排队
        if (userBindService.containsKey(myUserId)) return;

        waitingUsers.add(myUserId);
        // 计算位置
        int position = 0;
        int i = 0;
        for (Integer uid : waitingUsers) {
            i++;
            if (uid.equals(myUserId)) { position = i; break; }
        }

        JSONObject resp = new JSONObject();
        resp.put("type", "user_waiting");
        resp.put("position", position);
        resp.put("content", "已进入排队");
        sendMsg(session, resp.toJSONString());

        // 尝试立即匹配
        tryMatch();

        // 广播队列位置更新
        broadcastQueueCount();
    }

    /** 用户取消排队/结束会话 */
    private void handleUserCancel() {
        waitingUsers.remove(myUserId);
        if (userBindService.containsKey(myUserId)) {
            int serviceId = userBindService.remove(myUserId);
            serviceBindUser.remove(serviceId);
            // 客服回归空闲
            if (!freeServiceQueue.contains(serviceId)) freeServiceQueue.offer(serviceId);
            // 通知客服
            Session s = allSessionMap.get(serviceId);
            if (s != null) {
                JSONObject leave = new JSONObject();
                leave.put("type", "user_leave");
                leave.put("content", "用户已离开");
                leave.put("fromUserId", myUserId);
                sendMsg(s, leave.toJSONString());
            }
            // 客服可能有排队用户在等，立即匹配
            tryMatch();
        }
        JSONObject resp = new JSONObject();
        resp.put("type", "user_cancel_success");
        sendMsg(allSessionMap.get(myUserId), resp.toJSONString());
        broadcastQueueCount();
    }

    /** 客服手动接下一位 */
    private void handleServicePull() {
        if (!amService) return;
        // 如果正在服务中，先结束当前会话
        if (serviceBindUser.containsKey(myUserId)) {
            handleServiceFinish();
        }
        tryMatch();
    }

    /** 客服结束当前会话 */
    private void handleServiceFinish() {
        if (!amService) return;
        Integer userId = serviceBindUser.remove(myUserId);
        if (userId != null) {
            userBindService.remove(userId);
            Session userSession = allSessionMap.get(userId);
            if (userSession != null) {
                JSONObject leave = new JSONObject();
                leave.put("type", "user_leave");
                leave.put("content", "客服已结束会话");
                sendMsg(userSession, leave.toJSONString());
            }
        }
        // 客服回归空闲队列
        if (!freeServiceQueue.contains(myUserId)) freeServiceQueue.offer(myUserId);
        // 通知客服进入空闲
        Session ss = allSessionMap.get(myUserId);
        if (ss != null) {
            JSONObject idle = new JSONObject();
            idle.put("type", "service_idle");
            sendMsg(ss, idle.toJSONString());
        }
        // 立即尝试接下一位
        tryMatch();
    }

    /** 状态查询（页面刷新恢复） */
    private void handleStateQuery(Session session) {
        JSONObject state = new JSONObject();
        String myState;
        int peerId = 0;
        String peerName = "";

        if (amService) {
            if (serviceBindUser.containsKey(myUserId)) {
                myState = "chatting";
                peerId = serviceBindUser.get(myUserId);
            } else {
                myState = "idle";
            }
        } else {
            if (userBindService.containsKey(myUserId)) {
                myState = "chatting";
                peerId = userBindService.get(myUserId);
            } else if (waitingUsers.contains(myUserId)) {
                myState = "waiting";
            } else {
                myState = "idle";
            }
        }

        if (peerId > 0) {
            User u = userMapper.selectById(peerId);
            peerName = u != null ? u.getUsername() : "用户" + peerId;
        }

        state.put("state", myState);
        state.put("peerId", peerId);
        state.put("peerName", peerName);
        state.put("queueCount", waitingUsers.size());

        JSONObject resp = new JSONObject();
        resp.put("type", "state_response");
        resp.put("content", state.toJSONString());
        resp.put("queueCount", waitingUsers.size());
        sendMsg(session, resp.toJSONString());
    }

    /** 客服聊天中的消息转发 */
    private void handleServiceChat(JSONObject dto) {
        String content = dto.getString("content");
        if (content == null || content.trim().isEmpty()) return;

        int targetId;
        String fromLabel;
        if (amService) {
            // 客服发给当前绑定的用户
            Integer uid = serviceBindUser.get(myUserId);
            if (uid == null) return;
            targetId = uid;
            fromLabel = "service";
        } else {
            // 用户发给绑定的客服
            Integer sid = userBindService.get(myUserId);
            if (sid == null) return;
            targetId = sid;
            fromLabel = "user";
        }

        // 持久化
        saveMessage(myUserId, targetId, content, "chat");

        // 转发给对方
        Session targetSession = allSessionMap.get(targetId);
        if (targetSession != null) {
            JSONObject forward = new JSONObject();
            forward.put("type", "chat");
            forward.put("content", content);
            forward.put("from", fromLabel);
            forward.put("fromUserId", myUserId);
            forward.put("time", nowStr());
            sendMsg(targetSession, forward.toJSONString());
        }
        // 给自己也回一条（前端显示用）
        Session self = allSessionMap.get(myUserId);
        if (self != null) {
            JSONObject echo = new JSONObject();
            echo.put("type", "chat");
            echo.put("content", content);
            echo.put("from", fromLabel);
            echo.put("fromUserId", myUserId);
            echo.put("time", nowStr());
            sendMsg(self, echo.toJSONString());
        }
    }

    /** P2P 用户间直接聊天（不经过客服） */
    private void handleP2PChat(JSONObject dto) {
        Integer toUserId = dto.getInteger("toUserId");
        String content = dto.getString("content");
        if (toUserId == null || content == null || content.trim().isEmpty()) return;
        if (toUserId.equals(myUserId)) return;

        // 持久化
        saveMessage(myUserId, toUserId, content, "chat");

        // 构建消息包
        JSONObject forward = new JSONObject();
        forward.put("type", "chat");
        forward.put("content", content);
        forward.put("from", "other");
        forward.put("fromUserId", myUserId);
        forward.put("toUserId", toUserId);
        forward.put("peerType", "p2p");
        forward.put("time", nowStr());

        // 推给对方（如果在线）
        Session targetSession = allSessionMap.get(toUserId);
        if (targetSession != null) {
            sendMsg(targetSession, forward.toJSONString());
        }

        // 回显给自己
        Session self = allSessionMap.get(myUserId);
        if (self != null) {
            JSONObject echo = new JSONObject();
            echo.put("type", "chat");
            echo.put("content", content);
            echo.put("from", "me");
            echo.put("fromUserId", myUserId);
            echo.put("toUserId", toUserId);
            echo.put("peerType", "p2p");
            echo.put("time", nowStr());
            sendMsg(self, echo.toJSONString());
        }

        // 通知双方更新未读数
        pushUnreadNotify(toUserId);
    }

    private void handleTyping(JSONObject dto) {
        // 可扩展：输入中状态
    }

    // ==================== 工具方法 ====================

    /** 尝试匹配：空闲客服 ↔ 排队用户 */
    private void tryMatch() {
        while (!freeServiceQueue.isEmpty() && !waitingUsers.isEmpty()) {
            int serviceId = freeServiceQueue.poll();
            // 找到队列中第一个仍在线的用户
            Integer userId = null;
            for (Integer uid : waitingUsers) {
                if (allSessionMap.containsKey(uid)) {
                    userId = uid;
                    break;
                }
            }
            if (userId == null) {
                // 全部离线，清空队列
                waitingUsers.clear();
                break;
            }
            waitingUsers.remove(userId);

            // 绑定
            userBindService.put(userId, serviceId);
            serviceBindUser.put(serviceId, userId);

            User su = userMapper.selectById(serviceId);
            User uu = userMapper.selectById(userId);
            String sName = su != null ? su.getUsername() : "客服";
            String uName = uu != null ? uu.getUsername() : "用户";

            // 通知用户
            Session userSess = allSessionMap.get(userId);
            if (userSess != null) {
                JSONObject m = new JSONObject();
                m.put("type", "match_success");
                m.put("peerId", serviceId);
                m.put("peerName", sName);
                m.put("from", "service");
                sendMsg(userSess, m.toJSONString());
            }
            // 通知客服
            Session svcSess = allSessionMap.get(serviceId);
            if (svcSess != null) {
                JSONObject m = new JSONObject();
                m.put("type", "match_success");
                m.put("peerId", userId);
                m.put("peerName", uName);
                m.put("from", "user");
                sendMsg(svcSess, m.toJSONString());
            }
        }
    }

    /** 广播队列人数 */
    private void broadcastQueueCount() {
        // 可省略，state_query 会返回最新值
    }

    /** 推送未读数通知给指定用户 */
    public static void pushUnreadNotify(int userId) {
        Session s = allSessionMap.get(userId);
        if (s == null) return;
        try {
            // 查未读总数
            List<Object[]> groups = chatMessageMapper.selectUnreadCountByFrom(userId);
            int total = 0;
            for (Object[] g : groups) total += ((Number) g[1]).intValue();

            JSONObject notify = new JSONObject();
            notify.put("type", "notify_unread");
            notify.put("totalUnread", total);
            notify.put("content", "您有新的消息");
            sendMsgStatic(s, notify.toJSONString());
        } catch (Exception e) {
            log.warn("pushUnreadNotify error: {}", e.getMessage());
        }
    }

    /** 保存消息到数据库 */
    private void saveMessage(int from, int to, String content, String msgType) {
        try {
            ChatMessage m = new ChatMessage();
            m.setFromUserId(from);
            m.setToUserId(to);
            m.setContent(content);
            m.setMsgType(msgType);
            m.setIsRead(0);
            m.setSendDatetime(new Date());
            chatMessageMapper.insert(m);
        } catch (Exception e) {
            log.warn("saveMessage error: {}", e.getMessage());
        }
    }

    /** 断开时清理 */
    private void cleanupOnDisconnect() {
        allSessionMap.remove(myUserId);
        waitingUsers.remove(myUserId);
        // 如果是客服且正在服务，处理用户
        if (amService && serviceBindUser.containsKey(myUserId)) {
            Integer userId = serviceBindUser.remove(myUserId);
            if (userId != null) {
                userBindService.remove(userId);
                Session userSess = allSessionMap.get(userId);
                if (userSess != null) {
                    JSONObject leave = new JSONObject();
                    leave.put("type", "user_leave");
                    leave.put("content", "客服已下线");
                    sendMsgStatic(userSess, leave.toJSONString());
                }
            }
        }
        // 如果是用户且正在服务
        if (!amService && userBindService.containsKey(myUserId)) {
            Integer sid = userBindService.remove(myUserId);
            serviceBindUser.remove(sid);
            if (!freeServiceQueue.contains(sid)) freeServiceQueue.offer(sid);
            Session svc = allSessionMap.get(sid);
            if (svc != null) {
                JSONObject leave = new JSONObject();
                leave.put("type", "user_leave");
                leave.put("content", "用户已下线");
                sendMsgStatic(svc, leave.toJSONString());
            }
            tryMatch();
        }
        freeServiceQueue.remove(myUserId);
        log.info("[WS] 清理完成 userId={}, 在线={}, 客服空闲={}, 排队={}",
                myUserId, allSessionMap.size(), freeServiceQueue.size(), waitingUsers.size());
    }

    /** 向 session 发送消息 */
    private void sendMsg(Session session, String json) {
        if (session == null) return;
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(json);
            }
        } catch (IOException e) {
            log.warn("sendMsg fail: {}", e.getMessage());
        }
    }

    /** static 版本（给 pushUnreadNotify 用） */
    private static void sendMsgStatic(Session session, String json) {
        if (session == null) return;
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(json);
            }
        } catch (IOException e) {
            log.warn("sendMsgStatic fail: {}", e.getMessage());
        }
    }

    private void closeQuietly(Session session, int code, String reason) {
        try { session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(code), reason)); } catch (Exception ignored) {}
    }

    private static String nowStr() {
        Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d %02d:%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }
}
