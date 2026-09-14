package com.svtu.websocket;

import com.alibaba.fastjson.JSON;
import com.svtu.config.RabbitConfig;
import com.svtu.entity.ChatMessage;
import com.svtu.entity.User;
import com.svtu.mapper.ChatMessageMapper;
import com.svtu.mapper.UserMapper;
import com.svtu.mapper.UserRoleMapper;
import com.svtu.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 客服聊天 WebSocket 端点
 *
 * 排队机制：
 *  - 用户申请排队 → 发送消息到 RabbitMQ 队列 user_wait_queue（持久化、FIFO）
 *  - 客服上线 / 结束会话 / 用户入队时 → 主动从队列 receive 拉取一条进行匹配
 *  - 拉取时校验用户仍在线且仍在等待（取消/离线的消息作废跳过）
 *
 * 注意：@ServerEndpoint 每连接 new 一个实例（WebSocket 容器管理），
 * Spring 依赖必须通过静态字段 + setter 注入。
 */
@Slf4j
@Component
@ServerEndpoint("/ws/chat/{token}")
public class ChatWebSocket {

    // ===== Spring 依赖（静态注入，@ServerEndpoint 实例共享） =====
    private static JwtUtil jwtUtil;
    private static UserMapper userMapper;
    private static UserRoleMapper userRoleMapper;
    private static RabbitTemplate rabbitTemplate;
    private static ChatMessageMapper chatMessageMapper;

    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) { ChatWebSocket.jwtUtil = jwtUtil; }
    @Autowired
    public void setUserMapper(UserMapper userMapper) { ChatWebSocket.userMapper = userMapper; }
    @Autowired
    public void setUserRoleMapper(UserRoleMapper userRoleMapper) { ChatWebSocket.userRoleMapper = userRoleMapper; }
    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) { ChatWebSocket.rabbitTemplate = rabbitTemplate; }
    @Autowired
    public void setChatMessageMapper(ChatMessageMapper chatMessageMapper) { ChatWebSocket.chatMessageMapper = chatMessageMapper; }

    // ===== 全局状态 =====
    /** 所有在线连接：userId -> Session */
    private static final Map<Integer, Session> allSessionMap = new ConcurrentHashMap<>();
    /** 在线客服 id 集合 */
    private static final Set<Integer> onlineServices = ConcurrentHashMap.newKeySet();
    /** 空闲客服队列（未在服务中的客服） */
    private static final Queue<Integer> freeServiceQueue = new ConcurrentLinkedQueue<>();
    /** 排队中的用户（保序，用于计算排队位置） */
    private static final Set<Integer> waitingUsers = Collections.synchronizedSet(new LinkedHashSet<>());
    /** 用户 -> 客服 绑定（1v1 会话） */
    private static final Map<Integer, Integer> userBindService = new ConcurrentHashMap<>();
    /** 客服 -> 用户 绑定 */
    private static final Map<Integer, Integer> serviceBindUser = new ConcurrentHashMap<>();

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // ===== 当前连接实例的状态 =====
    private Session session;
    private int userId;
    private String username;
    private boolean isService;

    /**
     * 连接建立：token 放在 URL 路径上 /ws/chat/{token}
     */
    @OnOpen
    public void onOpen(@PathParam("token") String token, Session session) {
        String phone;
        try {
            phone = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            log.warn("WebSocket 连接 token 无效");
            closeQuietly(session);
            return;
        }
        User user = userMapper.selectUserId(phone);
        if (user == null) {
            log.warn("WebSocket 连接用户不存在：{}", phone);
            closeQuietly(session);
            return;
        }

        this.session = session;
        this.userId = user.getUserId();
        this.username = user.getUsername();

        List<String> roles = userRoleMapper.selectUserRoleName(this.userId);
        this.isService = roles != null && (roles.contains("客服") || roles.contains("管理员"));

        allSessionMap.put(this.userId, session);

        if (this.isService) {
            onlineServices.add(this.userId);
            // 客服上线：若不在会话中则加入空闲队列
            if (!serviceBindUser.containsKey(this.userId) && !freeServiceQueue.contains(this.userId)) {
                freeServiceQueue.offer(this.userId);
            }
            sendMsg(session, new MsgDTO("service_online", "客服已上线"));
            // 断线重连：若会话还在，重发匹配成功通知
            Integer uid = serviceBindUser.get(this.userId);
            if (uid != null) {
                notifyMatch(this.userId, uid, "user");
            }
            // 尝试接入排队用户
            tryMatch();
        } else {
            // 用户重连 / 页面刷新：发状态恢复包
            com.alibaba.fastjson.JSONObject state = new com.alibaba.fastjson.JSONObject();
            String subState;
            Integer pid = null;
            if (userBindService.containsKey(this.userId)) {
                subState = "chatting";
                pid = userBindService.get(this.userId);
            } else if (waitingUsers.contains(this.userId)) {
                subState = "waiting";
            } else {
                subState = "idle";
            }
            state.put("state", subState);
            state.put("peerId", pid);
            state.put("peerName", pid != null ? (allSessionMap.containsKey(pid) ? "客服" : null) : null);
            sendMsg(session, new MsgDTO("user_online", state.toJSONString()));
            log.info("用户上线：userId={}, state={}, peerId={}", this.userId, subState, pid);
            // 如果还在会话中，通知对方自己在线
            if ("chatting".equals(subState)) {
                notifyMatch(this.userId, pid, "service");
            }
        }
        broadcastQueueCount();
        log.info("WebSocket 连接成功：userId={}, isService={}", this.userId, this.isService);
    }

    /**
     * 接收前端消息
     */
    @OnMessage
    public void onMessage(String message) {
        log.info("收到消息：{}", message);
        MsgDTO dto;
        try {
            dto = JSON.parseObject(message, MsgDTO.class);
        } catch (Exception e) {
            return;
        }
        if (dto == null || dto.getType() == null) return;

        switch (dto.getType()) {
            case "user_state_query":
                handleStateQuery();
                break;
            case "user_apply":
                handleUserApply();
                break;
            case "user_cancel":
                handleUserCancel();
                break;
            case "service_pull":
                // 客服手动"接入下一位"：结束当前会话 → 重新进入空闲队列 → tryMatch 接下一位
                if (isService) {
                    handleServiceFinish(); // 释放当前会话，客服回到 freeServiceQueue
                    tryMatch();           // 从 freeServiceQueue 里现在就有这个客服了，立刻接下一个排队用户
                    broadcastQueueCount();
                }
                break;
            case "service_finish":
                handleServiceFinish();
                break;
            case "chat":
                handleChat(dto.getContent());
                break;
            case "user_chat":
                // P2P 用户间聊天（一对一，无客服绑定）
                handleP2PChat(dto.getToUserId(), dto.getContent());
                break;
            default:
                break;
        }
    }

    /**
     * 查询当前会话状态（前端页面重建 / 刷新时恢复状态用）
     */
    private void handleStateQuery() {
        com.alibaba.fastjson.JSONObject state = new com.alibaba.fastjson.JSONObject();
        String subState;
        Integer pid = null;
        if (isService) {
            Integer uid = serviceBindUser.get(userId);
            if (uid != null) {
                subState = "chatting";
                pid = uid;
            } else {
                subState = "idle";
            }
            state.put("state", subState);
            state.put("peerId", pid);
            state.put("queueCount", waitingUsers.size());
        } else {
            if (userBindService.containsKey(userId)) {
                subState = "chatting";
                pid = userBindService.get(userId);
            } else if (waitingUsers.contains(userId)) {
                subState = "waiting";
            } else {
                subState = "idle";
            }
            state.put("state", subState);
            state.put("peerId", pid);
        }
        sendMsg(session, new MsgDTO("state_response", state.toJSONString()));
        log.info("状态查询：userId={}, isService={}, resp={}", userId, isService, state);
    }

    /**
     * 用户申请排队
     */
    private void handleUserApply() {
        if (isService) return;
        if (userBindService.containsKey(userId)) {
            sendMsg(session, new MsgDTO("already_bound", "您已在客服会话中"));
            return;
        }
        // 未排队才入队（重复点击不重复发 MQ）
        if (!waitingUsers.contains(userId)) {
            waitingUsers.add(userId);
            try {
                rabbitTemplate.convertAndSend(RabbitConfig.USER_EXCHANGE, "user.key", String.valueOf(userId));
            } catch (Exception e) {
                log.warn("MQ 发送失败，用户仍在内存等待队列：userId={}, err={}", userId, e.getMessage());
            }
        }
        sendPosition(userId);
        // 尝试匹配（即使 MQ 没消息，也会从 waitingUsers 内存集合兜底匹配）
        tryMatch();
        broadcastQueueCount();
    }

    /**
     * 用户取消排队 / 结束会话
     */
    private void handleUserCancel() {
        boolean removed = waitingUsers.remove(userId);
        Integer sid = userBindService.remove(userId);
        if (sid != null) {
            serviceBindUser.remove(sid);
            // 客服回归空闲，自动接入下一位
            if (!freeServiceQueue.contains(sid)) freeServiceQueue.offer(sid);
            sendMsg(allSessionMap.get(sid), new MsgDTO("user_leave", "用户已结束会话"));
            tryMatch();
        }
        if (removed || sid != null) {
            sendMsg(session, new MsgDTO("user_cancel_success", "已退出排队/会话"));
        }
        broadcastPosition();
        broadcastQueueCount();
    }

    /**
     * 客服结束会话
     */
    private void handleServiceFinish() {
        if (!isService) return;
        Integer uid = serviceBindUser.remove(userId);
        if (uid != null) {
            userBindService.remove(uid);
            sendMsg(allSessionMap.get(uid), new MsgDTO("service_leave", "客服已结束会话，如有需要可重新排队"));
        }
        if (!freeServiceQueue.contains(userId)) freeServiceQueue.offer(userId);
        sendMsg(session, new MsgDTO("service_idle", "会话已结束，您已回到空闲状态"));
        tryMatch();
        broadcastQueueCount();
    }

    /**
     * 转发聊天消息并落库
     */
    private void handleChat(String content) {
        if (content == null || content.trim().isEmpty()) return;
        String time = SDF.format(new Date());
        if (isService) {
            Integer uid = serviceBindUser.get(userId);
            if (uid == null) return;
            saveMessage(userId, uid, content);
            MsgDTO dto = new MsgDTO("chat", content.trim(), "service");
            dto.setTime(time);
            sendMsg(allSessionMap.get(uid), dto);
        } else {
            Integer sid = userBindService.get(userId);
            if (sid == null) return;
            saveMessage(userId, sid, content);
            MsgDTO dto = new MsgDTO("chat", content.trim(), "user");
            dto.setTime(time);
            sendMsg(allSessionMap.get(sid), dto);
        }
    }

    /**
     * P2P 用户间聊天：一对一转发 + 落库 + 回推发送方
     * 不依赖客服绑定表，直接通过 toUserId 在 allSessionMap 中查找对方 session
     */
    private void handleP2PChat(int toUserId, String content) {
        if (content == null || content.trim().isEmpty()) return;
        if (toUserId <= 0 || toUserId == this.userId) return;
        String time = SDF.format(new Date());

        // 1. 落库
        saveMessage(this.userId, toUserId, content.trim());

        // 2. 构造推送 DTO（推给双方的消息体一致，fromUserId/toUserId 固定）
        MsgDTO dto = new MsgDTO("chat", content.trim());
        dto.setPeerType("p2p");
        dto.setFromUserId(this.userId);
        dto.setToUserId(toUserId);
        dto.setTime(time);

        // 3. 推给接收方（如果对方在线）
        Session receiver = allSessionMap.get(toUserId);
        if (receiver != null && receiver.isOpen()) {
            sendMsg(receiver, dto);
        }

        // 4. 回推发送方（乐观更新，让发送方立刻看到自己发的消息）
        sendMsg(this.session, dto);
    }

    /**
     * 匹配：空闲客服从 RabbitMQ 队列按 FIFO 拉取排队用户
     *        兜底：MQ 队列为空时从 waitingUsers 内存集合匹配
     */
    private static synchronized void tryMatch() {
        while (!freeServiceQueue.isEmpty()) {
            // 1. 先尝试从 RabbitMQ 拉取
            Object obj = null;
            try {
                obj = rabbitTemplate.receiveAndConvert(RabbitConfig.USER_QUEUE, 300);
            } catch (Exception e) {
                log.warn("MQ 拉取异常，回退内存匹配：{}", e.getMessage());
            }

            Integer uid = null;
            if (obj != null) {
                try { uid = Integer.parseInt(obj.toString().trim()); }
                catch (NumberFormatException e) { /* 跳过 */ }
            }

            // 2. MQ 没消息 → 从 waitingUsers 内存集合找第一个有效排队用户
            if (uid == null) {
                for (Integer candidate : waitingUsers) {
                    Session cs = allSessionMap.get(candidate);
                    if (cs != null && cs.isOpen() && !userBindService.containsKey(candidate)) {
                        uid = candidate;
                        break;
                    }
                }
                if (uid == null) return; // 真没人排队
            }

            Session uSession = allSessionMap.get(uid);
            // 校验：用户仍在线、仍在等待、未绑定（取消排队/离线的消息作废）
            if (uSession == null || !uSession.isOpen()
                    || !waitingUsers.contains(uid)
                    || userBindService.containsKey(uid)) {
                waitingUsers.remove(uid);
                continue;
            }

            Integer sid = freeServiceQueue.poll();
            if (sid == null) return;
            Session sSession = allSessionMap.get(sid);
            if (sSession == null || !sSession.isOpen()) {
                // 客服已离线：放回空闲队列，消息放回等待
                freeServiceQueue.offer(sid);
                continue;
            }

            // 绑定会话
            waitingUsers.remove(uid);
            userBindService.put(uid, sid);
            serviceBindUser.put(sid, uid);

            notifyMatch(uid, sid, "service");
            notifyMatch(sid, uid, "user");
            log.info("匹配成功：用户 {} ↔ 客服 {}", uid, sid);
        }
        broadcastPosition();
        broadcastQueueCount();
    }

    /**
     * 给一方发送"匹配成功"通知
     * @param receiverId 接收者
     * @param peerId     对方id
     * @param peerRole   对方角色：service=对方是客服 / user=对方是用户
     */
    private static void notifyMatch(int receiverId, int peerId, String peerRole) {
        User peer = userMapper.selectById(peerId);
        MsgDTO dto = new MsgDTO("match_success", "match");
        dto.setPeerId(peerId);
        dto.setPeerName(peer != null ? peer.getUsername() : "");
        dto.setFrom(peerRole);
        sendMsg(allSessionMap.get(receiverId), dto);
    }

    /**
     * 给指定用户发送排队位置
     */
    private static void sendPosition(int targetUserId) {
        int pos = 0;
        synchronized (waitingUsers) {
            for (Integer id : waitingUsers) {
                pos++;
                if (id.equals(targetUserId)) break;
            }
        }
        MsgDTO dto = new MsgDTO("user_waiting", "已进入排队，请等待客服接入");
        dto.setPosition(pos);
        sendMsg(allSessionMap.get(targetUserId), dto);
    }

    /**
     * 广播排队位置给所有排队用户
     */
    private static void broadcastPosition() {
        int pos = 0;
        synchronized (waitingUsers) {
            for (Integer id : waitingUsers) {
                pos++;
                MsgDTO dto = new MsgDTO("position", null);
                dto.setPosition(pos);
                sendMsg(allSessionMap.get(id), dto);
            }
        }
    }

    /**
     * 广播排队人数给所有在线客服
     */
    private static void broadcastQueueCount() {
        MsgDTO dto = new MsgDTO("queue_count", null);
        dto.setPosition(waitingUsers.size());
        for (Integer sid : onlineServices) {
            sendMsg(allSessionMap.get(sid), dto);
        }
    }

    /**
     * 聊天消息落库
     */
    private static void saveMessage(int fromUserId, int toUserId, String content) {
        try {
            ChatMessage msg = new ChatMessage();
            msg.setFromUserId(fromUserId);
            msg.setToUserId(toUserId);
            msg.setContent(content);
            msg.setIsRead(0);  // 初始未读（接收方标已读后会 update）
            msg.setSendDatetime(new Date());
            chatMessageMapper.insert(msg);
        } catch (Exception e) {
            log.error("聊天消息落库失败", e);
        }
    }

    /**
     * 发送消息工具
     */
    private static void sendMsg(Session session, MsgDTO dto) {
        try {
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(JSON.toJSONString(dto));
            }
        } catch (Exception e) {
            log.error("WebSocket 发送消息失败", e);
        }
    }

    private static void closeQuietly(Session session) {
        try {
            if (session != null && session.isOpen()) session.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * 连接关闭：清理状态，释放客服资源
     */
    @OnClose
    public void onClose() {
        if (userId == 0) return;
        allSessionMap.remove(userId);
        log.info("WebSocket 断开：userId={}, isService={}", userId, isService);

        if (isService) {
            onlineServices.remove(userId);
            freeServiceQueue.remove(userId);
            Integer uid = serviceBindUser.remove(userId);
            if (uid != null) {
                userBindService.remove(uid);
                sendMsg(allSessionMap.get(uid), new MsgDTO("service_leave", "客服已离线，请稍后重新排队"));
            }
        } else {
            waitingUsers.remove(userId);
            Integer sid = userBindService.remove(userId);
            if (sid != null) {
                serviceBindUser.remove(sid);
                if (!freeServiceQueue.contains(sid)) freeServiceQueue.offer(sid);
                sendMsg(allSessionMap.get(sid), new MsgDTO("user_leave", "用户已离开"));
                // 客服空闲出来，自动接下一位排队用户
                tryMatch();
            }
        }
        broadcastPosition();
        broadcastQueueCount();
    }

    @OnError
    public void onError(Throwable error) {
        log.error("WebSocket 异常：userId={}", userId, error);
    }

    /**
     * WebSocket 消息体
     */
    public static class MsgDTO {
        private String type;       // 消息类型
        private String content;    // 内容
        private String from;       // user / service（客服会话语义；P2P 场景不依赖此字段）
        private int userId;        // 兼容旧字段（客服会话发送方）
        private int fromUserId;    // P2P 发送方 userId（通用）
        private int toUserId;      // P2P 接收方 userId（通用）
        private String peerType;   // service=客服会话 / p2p=用户间 P2P
        private int peerId;        // 会话对方id
        private String peerName;   // 会话对方昵称
        private int position;      // 排队位置/人数
        private String time;       // 消息时间
        private String token;
        private String cmd;

        public MsgDTO() {}
        public MsgDTO(String type, String content) {
            this.type = type;
            this.content = content;
        }
        public MsgDTO(String type, String content, String from) {
            this.type = type;
            this.content = content;
            this.from = from;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        public int getFromUserId() { return fromUserId; }
        public void setFromUserId(int fromUserId) { this.fromUserId = fromUserId; }
        public int getToUserId() { return toUserId; }
        public void setToUserId(int toUserId) { this.toUserId = toUserId; }
        public String getPeerType() { return peerType; }
        public void setPeerType(String peerType) { this.peerType = peerType; }
        public int getPeerId() { return peerId; }
        public void setPeerId(int peerId) { this.peerId = peerId; }
        public String getPeerName() { return peerName; }
        public void setPeerName(String peerName) { this.peerName = peerName; }
        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getCmd() { return cmd; }
        public void setCmd(String cmd) { this.cmd = cmd; }
    }
}
