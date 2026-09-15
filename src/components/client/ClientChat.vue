<template>
  <el-container class="chat-page">
    <el-header class="chat-header">
      <el-button @click="goBack" :icon="ArrowLeft" circle />
      <span class="header-title">在线客服</span>
      <el-tag v-if="status === 'chatting'" type="success" size="small" class="header-tag">
        客服：{{ peerName }}
      </el-tag>
      <el-tag v-else-if="status === 'waiting'" type="warning" size="small" class="header-tag">排队中</el-tag>
    </el-header>

    <el-main class="chat-main">
      <el-card shadow="hover" class="chat-card">
        <div class="chat-body">
          <!-- 未排队 -->
          <div v-if="status === 'idle'" class="center-area">
            <el-icon :size="48" color="#4080FF"><Service /></el-icon>
            <p class="center-title">有问题？联系在线客服</p>
            <p class="center-desc">咨询人数较多时需排队等待，请耐心等候</p>
            <el-button type="primary" size="large" :loading="connecting" @click="applyQueue">
              <el-icon class="btn-icon"><ChatDotRound /></el-icon>联系客服
            </el-button>
          </div>

          <!-- 排队中 -->
          <div v-else-if="status === 'waiting'" class="center-area">
            <el-icon :size="48" color="#E6A23C" class="is-loading"><Loading /></el-icon>
            <p class="center-title">正在为您排队接入客服…</p>
            <p class="waiting-desc">
              您前面还有 <b class="waiting-count">{{ position - 1 <= 0 ? 0 : position - 1 }}</b> 人
              <span class="waiting-pos">（您是第 {{ position }} 位）</span>
            </p>
            <el-button @click="cancelQueue">取消排队</el-button>
          </div>

          <!-- 聊天中 -->
          <template v-else>
            <div class="msg-box" ref="msgBoxRef">
              <div v-if="msgList.length === 0" class="empty-tip">
                已接入客服，请描述您的问题
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
                placeholder="输入消息，Enter 发送，Shift+Enter 换行"
                @keydown.enter.exact.prevent="sendMessage"
              />
              <el-button type="primary" :disabled="!sendContent.trim()" @click="sendMessage">发送</el-button>
            </div>
            <div class="end-chat-row">
              <el-button type="danger" link size="small" @click="endChat">结束会话</el-button>
            </div>
          </template>
        </div>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Service, ChatDotRound, Loading } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { connectChat, sendChat, onChat, offChat, closeChat } from '../../utils/ws'
import { getChatHistory, markChatRead } from '../../api/chat'
import { useChatStore } from '../../stores/chat'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

// 会话结束后清掉对方发来的未读消息通知
const clearPeerUnread = async (pid) => {
  if (!pid) return
  try { await markChatRead({ fromUserId: pid }) } catch (e) {}
  chatStore.fetchUnread()
}

const connecting = ref(false)
const status = ref('idle')   // idle | waiting | chatting
const position = ref(0)
const peerId = ref(0)
const peerName = ref('')
const msgList = ref([])
const sendContent = ref('')
const msgBoxRef = ref(null)

const goBack = () => router.push('/clientMain')

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

// 申请排队
const applyQueue = () => {
  connecting.value = true
  const ok = sendChat('user_apply')
  if (!ok) {
    ElMessage.error('连接未建立，正在重试…')
    setTimeout(() => sendChat('user_apply'), 1500)
  }
  setTimeout(() => (connecting.value = false), 1000)
}

// 取消排队
const cancelQueue = () => {
  sendChat('user_cancel')
  status.value = 'idle'
}

// 结束会话
const endChat = () => {
  ElMessageBox.confirm('确定结束与客服的会话吗？', '提示', { type: 'warning' })
    .then(async () => {
      const pid = peerId.value
      sendChat('user_cancel')
      status.value = 'idle'
      msgList.value = []
      peerId.value = 0
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
  msgList.value.push({ from: 'user', content: text, time: now() })
  sendContent.value = ''
  scrollBottom()
}

// 拉取历史消息
const loadHistory = async (pid) => {
  try {
    const res = await getChatHistory({ peerId: pid })
    msgList.value = (res.data || []).map((m) => ({
      from: m.fromUserId === userStore.userId ? 'user' : 'service',
      content: m.content,
      time: m.sendDatetime ? m.sendDatetime.replace('T', ' ').substring(0, 16) : ''
    }))
    scrollBottom()
  } catch (e) {
    // 历史拉取失败不阻塞
  }
}

// ===== WebSocket 消息处理 =====
const handlers = {
  user_online: () => {
    // 连接成功后主动查询状态
    sendChat('user_state_query')
  },
  state_response: (msg) => {
    try {
      const st = JSON.parse(msg.content || msg.data || '{}')
      status.value = st.state || 'idle'
      if (st.state === 'chatting') {
        peerId.value = st.peerId || 0
        peerName.value = st.peerName || '客服'
        loadHistory(st.peerId)
      } else if (st.state === 'waiting') {
        position.value = msg.position || 1
      }
    } catch (e) {}
  },
  user_waiting: (msg) => {
    status.value = 'waiting'
    position.value = msg.position || 1
  },
  position: (msg) => {
    if (status.value === 'waiting') position.value = msg.position || 1
  },
  match_success: (msg) => {
    status.value = 'chatting'
    peerId.value = msg.peerId
    peerName.value = msg.peerName || '客服'
    ElMessage.success(`已接入客服：${peerName.value}`)
    loadHistory(msg.peerId)
  },
  chat: (msg) => {
    if (status.value !== 'chatting') return
    msgList.value.push({ from: msg.from, content: msg.content, time: msg.time || now() })
    scrollBottom()
  },
  service_leave: async (msg) => {
    ElMessage.info(msg.content || '客服已结束会话')
    const pid = peerId.value
    status.value = 'idle'
    msgList.value = []
    peerId.value = 0
    await clearPeerUnread(pid)
  },
  user_cancel_success: async () => {
    const pid = peerId.value
    status.value = 'idle'
    await clearPeerUnread(pid)
  }
}

onMounted(() => {
  const token = userStore.token || localStorage.getItem('token')
  connectChat(token)
  Object.keys(handlers).forEach((t) => onChat(t, handlers[t]))
  // 无论新连接还是复用已有连接，都主动查一次状态恢复
  // （复用连接时后端 onOpen 不会触发，需要主动问）
  setTimeout(() => sendChat('user_state_query'), 200)
})

onBeforeUnmount(() => {
  Object.keys(handlers).forEach((t) => offChat(t, handlers[t]))
  // 页面离开不断开（客服端需要），保留连接
})
</script>

<style scoped>
.chat-page {
  height: 100vh;
  background: var(--content-bg);
}
.chat-header {
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--border);
}
.header-title {
  margin-left: 12px;
  font-size: 18px;
  font-weight: bold;
}
.header-tag {
  margin-left: 12px;
}
.chat-main {
  padding: 20px;
  display: flex;
  justify-content: center;
}
.chat-card {
  width: 820px;
  display: flex;
  flex-direction: column;
}
.chat-body {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
}
.center-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}
.center-title {
  margin: 16px 0 8px;
  font-size: 16px;
  font-weight: 600;
}
.center-desc {
  color: #909399;
  font-size: 13px;
  margin-bottom: 20px;
}
.btn-icon {
  margin-right: 6px;
}
.waiting-desc {
  color: #E6A23C;
  font-size: 15px;
  margin-bottom: 20px;
}
.waiting-count {
  font-size: 20px;
}
.waiting-pos {
  color: #909399;
  font-size: 13px;
}
.empty-tip {
  text-align: center;
  color: #999;
  margin-top: 60px;
}
.msg-box {
  flex: 1;
  min-height: 380px;
  max-height: 55vh;
  overflow-y: auto;
  padding: 16px;
  background: var(--content-bg);
  border-radius: var(--radius);
  border: 1px solid var(--border);
}
.msg-row {
  margin-bottom: 16px;
  max-width: 70%;
  word-wrap: break-word;
}
.msg-row.user {
  margin-left: auto;
  text-align: right;
}
.msg-row.service {
  margin-right: auto;
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
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 6px 14px -6px rgba(64, 128, 255, .55);
}
.msg-row.service .msg-content {
  background: #fff;
  color: #303133;
  border: 1px solid var(--border-light);
  box-shadow: 0 2px 8px rgba(16, 24, 40, .05);
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
  padding: 0 4px 8px;
}
</style>