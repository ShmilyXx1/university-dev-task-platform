<template>
  <el-card shadow="hover" style="height: 86vh; display: flex; flex-direction: column;">
    <template #header>
      <div class="card-header">
        <span>客服工作台</span>
        <div>
          <el-tag type="warning" size="small" style="margin-right: 10px">
            排队等待：{{ waitCount }} 人
          </el-tag>
          <el-tag :type="status === 'chatting' ? 'success' : 'info'" size="small">
            {{ status === 'chatting' ? `服务中：${peerName}` : '空闲中' }}
          </el-tag>
        </div>
      </div>
    </template>

    <div style="flex: 1; display: flex; flex-direction: column;">
      <!-- 空闲状态 -->
      <div v-if="status === 'idle'" class="idle-area">
        <el-icon :size="48" color="#67C23A"><Service /></el-icon>
        <p style="margin: 16px 0 8px; font-size: 16px; font-weight: 600">您已上线，等待用户咨询</p>
        <p style="color: #909399; font-size: 13px; margin-bottom: 20px">
          有用户排队时会自动接入；当前等待 <b style="color:#E6A23C">{{ waitCount }}</b> 人
        </p>
        <el-button type="primary" size="large" @click="pullNext">
          <el-icon style="margin-right: 6px"><Refresh /></el-icon>手动接入下一位
        </el-button>
      </div>

      <!-- 服务中 -->
      <template v-else>
        <div class="msg-box" ref="msgBoxRef">
          <div v-if="msgList.length === 0" style="text-align:center; color:#999; margin-top: 60px">
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
        <div style="text-align: right; padding: 8px 4px 0">
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
import { getChatHistory } from '../../api/chat'

const userStore = useUserStore()

const status = ref('idle')      // idle | chatting
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
    .then(() => {
      sendChat('service_finish')
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
  user_leave: (msg) => {
    ElMessage.info(msg.content || '用户已离开')
    status.value = 'idle'
    msgList.value = []
    peerId.value = 0
  },
  service_idle: () => {
    status.value = 'idle'
    msgList.value = []
    peerId.value = 0
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
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}
.idle-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.msg-box {
  flex: 1;
  min-height: 380px;
  max-height: 60vh;
  overflow-y: auto;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #ebeef5;
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
  border-radius: 10px;
  line-height: 1.6;
  text-align: left;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-row.user .msg-content {
  background: #fff;
  border: 1px solid #e4e7ed;
}
.msg-row.service .msg-content {
  background: #409EFF;
  color: #fff;
}
.msg-time {
  font-size: 11px;
  color: #aaa;
  margin-top: 4px;
}
.send-area {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 12px 0 0;
}
</style>
