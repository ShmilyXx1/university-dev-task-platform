<template>
  <el-card shadow="hover" class="service-card">
    <template #header>
      <div class="card-header">
        <span class="card-title">客服工作台</span>
        <div class="header-tags">
          <el-tag type="warning" size="small" class="tag-mr">
            排队等待：{{ waitCount }} 人
          </el-tag>
          <el-tag :type="status === 'chatting' ? 'success' : 'info'" size="small">
            {{ status === 'chatting' ? `服务中：${peerName}` : '空闲中' }}
          </el-tag>
        </div>
      </div>
    </template>

    <div class="service-body">
      <!-- 空闲状态 -->
      <div v-if="status === 'idle'" class="idle-area">
        <el-icon :size="48" color="#67C23A"><Service /></el-icon>
        <p class="idle-title">您已上线，等待用户咨询</p>
        <p class="idle-desc">
          有用户排队时会自动接入；当前等待 <b class="highlight">{{ waitCount }}</b> 人
        </p>
        <el-button type="primary" size="large" @click="pullNext">
          <el-icon class="btn-icon"><Refresh /></el-icon>手动接入下一位
        </el-button>
      </div>

      <!-- 服务中 -->
      <template v-else>
        <div class="msg-box" ref="msgBoxRef">
          <div v-if="msgList.length === 0" class="empty-hint">
            用户 {{ peerName }} 已接入，请主动问候
          </div>
          <div v-for="(msg, idx) in msgList" :key="idx" class="msg-row" :class="msg.from">
            <div class="msg-content">{{ msg.content }}</div>
            <div class="msg-time">{{ msg.time }}</div>
          </div>
        </div>
        <div class="send-area">
          <el-input
            v-model="sendContent"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="500"
            placeholder="输入回复，Enter 发送，Shift+Enter 换行"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <el-button type="primary" :disabled="!sendContent.trim()" @click="sendMessage">发送</el-button>
        </div>
        <div class="end-chat-row">
          <el-button type="warning" size="small" @click="finishChat">结束会话并接入下一位</el-button>
        </div>
      </template>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Service, Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { connectChat, sendChat, onChat, offChat } from '../../utils/ws'
import { getChatHistory, markChatRead } from '../../api/chat'
import { useChatStore } from '../../stores/chat'

const userStore = useUserStore()
const chatStore = useChatStore()

// 会话结束后清掉对方发来的未读消息通知
const clearPeerUnread = async (pid) => {
  if (!pid) return
  try { await markChatRead({ fromUserId: pid }) } catch (e) {}
  chatStore.fetchUnread()
}

const status = ref('idle')      // idle | talking
const waitCount = ref(0)
const peerId = ref(0)
const peerName = ref('')
const msgList = ref([])
const sendContent = ref('')
const msgBoxRef = ref(null)

const now = () => {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const scrollBottom = () => {
  nextTick(() => {
    if (msgBoxRef.value) msgBoxRef.value.scrollTop = msgBoxRef.value.scrollHeight
  })
}

// 手动接入
const pullNext = () => {
  sendChat('service_pull')
}

// 结束会话
const finishChat = () => {
  ElMessageBox.confirm('结束当前会话？结束后将自动接入下一位排队用户。', '提示', { type: 'warning' })
    .then(async () => {
      const pid = peerId.value
      sendChat('service_finish')
      await clearPeerUnread(pid)
    }).catch(() => {})
}

// 发送消息
const sendMessage = () => {
  const text = sendContent.value.trim()
  if (!text) return
  if (!sendChat('chat', { content: text })) {
    ElMessage.error('连接已断开，正在重连…')
    return
  }
  msgList.value.push({ from: 'service', content: text, time: now() })
  sendContent.value = ''
  scrollBottom()
}

// 拉历史
const loadHistory = async (pid) => {
  try {
    const res = await getChatHistory({ peerId: pid })
    msgList.value = (res.data || []).map((m) => ({
      from: m.fromUserId === userStore.userId ? 'service' : 'user',
      content: m.content,
      time: m.sendDatetime ? m.sendDatetime.replace('T', ' ').substring(0, 16) : ''
    }))
    scrollBottom()
  } catch (e) {
    // 忽略
  }
}

// ===== WebSocket 消息 =====
const handlers = {
  service_online: () => {
    // 连接成功后主动查询状态
    sendChat('user_state_query')
  },
  state_response: (msg) => {
    try {
      const st = JSON.parse(msg.content || msg.data || '{}')
      waitCount.value = st.queueCount || 0
      if (st.state === 'chatting') {
        status.value = 'chatting'
        peerId.value = st.peerId || 0
        peerName.value = st.peerName || '用户'
        loadHistory(st.peerId)
      } else {
        status.value = 'idle'
        peerId.value = 0
        peerName.value = ''
      }
    } catch (e) {}
  },
  queue_count: (msg) => {
    waitCount.value = msg.position || 0
  },
  match_success: (msg) => {
    // from=user 表示对方是用户（客服收到的匹配通知）
    if (msg.from !== 'user') return
    status.value = 'chatting'
    peerId.value = msg.peerId
    peerName.value = msg.peerName || '用户'
    msgList.value = []
    ElMessage.success(`已接入用户：${peerName.value}`)
    loadHistory(msg.peerId)
  },
  chat: (msg) => {
    if (msg.from !== 'user') return
    msgList.value.push({ from: 'user', content: msg.content, time: msg.time || now() })
    scrollBottom()
  },
  user_leave: async (msg) => {
    ElMessage.info(msg.content || '用户已离开')
    const pid = peerId.value
    status.value = 'idle'
    msgList.value = []
    peerId.value = 0
    await clearPeerUnread(pid)
  },
  service_idle: async () => {
    const pid = peerId.value
    status.value = 'idle'
    msgList.value = []
    peerId.value = 0
    await clearPeerUnread(pid)
  }
}

onMounted(() => {
  const token = userStore.token || localStorage.getItem('token')
  connectChat(token)
  Object.keys(handlers).forEach((t) => onChat(t, handlers[t]))
  // 无论新连接还是复用已有连接，都主动查一次状态恢复
  setTimeout(() => sendChat('user_state_query'), 200)
})

onBeforeUnmount(() => {
  Object.keys(handlers).forEach((t) => offChat(t, handlers[t]))
})
</script>

<style scoped>
.service-card {
  height: 86vh;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-tags {
  display: flex;
  align-items: center;
}

.tag-mr {
  margin-right: 10px;
}

.btn-icon {
  margin-right: 6px;
}

.service-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.idle-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.idle-title {
  margin: 16px 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.idle-desc {
  color: #909399;
  font-size: 13px;
  margin-bottom: 20px;
}

.highlight {
  color: #E6A23C;
}

.empty-hint {
  text-align: center;
  color: #999;
  margin-top: 60px;
}

.msg-box {
  flex: 1;
  min-height: 380px;
  max-height: 60vh;
  overflow-y: auto;
  padding: 16px;
  background: var(--content-bg);
  border-radius: var(--radius);
  border: 1px solid var(--border);
}
.msg-row {
  margin-bottom: 16px;
  max-width: 70%;
}
.msg-row.user {
  margin-right: auto;
}
.msg-row.service {
  margin-left: auto;
  text-align: right;
}
.msg-content {
  display: inline-block;
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.6;
  text-align: left;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.msg-row.user .msg-content {
  background: #fff;
  border: 1px solid var(--border-light);
  color: #303133;
  box-shadow: 0 2px 8px rgba(16, 24, 40, .05);
}
.msg-row.service .msg-content {
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 6px 14px -6px rgba(64, 128, 255, .55);
}
.msg-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}
.send-area {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 12px 0 0;
}
.end-chat-row {
  text-align: right;
  padding: 8px 4px 0;
}
</style>
