<template>
  <el-container style="height: 100vh; background: #f5f7fa">
    <el-header style="background: #fff; display: flex; align-items: center; padding: 0 20px; border-bottom: 1px solid #e4e7ed">
      <el-button @click="goBack" :icon="ArrowLeft" circle />
      <span style="margin-left: 12px; font-size: 18px; font-weight: bold">
        与 {{ peerName || ('用户 ' + peerId) }} 聊天
      </span>
      <el-tag size="small" style="margin-left: 12px" :type="online ? 'success' : 'info'">
        {{ online ? '在线' : '离线' }}
      </el-tag>
    </el-header>

    <el-main style="padding: 20px; display: flex; justify-content: center">
      <el-card shadow="hover" style="width: 760px; display: flex; flex-direction: column">
        <div class="chat-body">
          <div class="msg-box" ref="msgBoxRef">
            <div v-if="msgList.length === 0" style="text-align:center; color:#999; margin-top: 60px">
              开始与 {{ peerName || '对方' }} 的对话吧
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
        </div>
      </el-card>
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { connectChat, sendChat, onChat, offChat } from '../../utils/ws'
import { getChatHistory, markChatRead } from '../../api/chat'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const peerId = computed(() => Number(route.query.peerId) || 0)
const peerName = computed(() => route.query.peerName || '')

const msgList = ref([])
const sendContent = ref('')
const msgBoxRef = ref(null)
const online = ref(false)

// 在线状态由对方发来的消息/自己发消息成功间接体现，也可以通过心跳检测更新

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

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/clientMain')
}

// 发送 P2P 消息
const sendMessage = () => {
  const text = sendContent.value.trim()
  if (!text) return
  if (!sendChat('user_chat', { toUserId: peerId.value, content: text })) {
    ElMessage.error('连接已断开，正在重连…')
    return
  }
  sendContent.value = ''
}

// 拉历史
const loadHistory = async () => {
  try {
    const res = await getChatHistory({ peerId: peerId.value })
    msgList.value = (res.data || []).map((m) => ({
      from: m.fromUserId === userStore.userId ? 'me' : 'other',
      content: m.content,
      time: m.sendDatetime ? m.sendDatetime.replace('T', ' ').substring(0, 16) : ''
    }))
    scrollBottom()
    // 标记对方发来的消息为已读
    if (peerId.value) {
      markChatRead({ fromUserId: peerId.value }).catch(() => {})
    }
  } catch (e) {}
}

const handlers = {
  user_online: () => {
    connectChat(userStore.token) // 确保连接建立
  },
  service_online: () => {
    connectChat(userStore.token)
  },
  chat: (msg) => {
    // 只处理 P2P 类型的消息
    if (msg.peerType === 'p2p') {
      const from = msg.fromUserId === userStore.userId ? 'me' : 'other'
      msgList.value.push({ from, content: msg.content, time: msg.time || now() })
      scrollBottom()
      // 如果是对方发来的 → 标记已读
      if (msg.fromUserId !== userStore.userId && msg.fromUserId === peerId.value) {
        markChatRead({ fromUserId: peerId.value }).catch(() => {})
      }
    }
  }
}

onMounted(async () => {
  if (!peerId.value) {
    ElMessage.error('缺少聊天对象参数')
    router.push('/clientMain')
    return
  }
  // 确保 WS 连接
  const token = userStore.token || localStorage.getItem('token')
  connectChat(token)
  // 注册监听
  Object.keys(handlers).forEach((t) => onChat(t, handlers[t]))
  // 加载历史
  await loadHistory()
  // 自己发消息能成功就说明自己在线；对方在线状态无法直接获取，通过消息是否送达间接体现
})

onBeforeUnmount(() => {
  Object.keys(handlers).forEach((t) => offChat(t, handlers[t]))
  // 页面离开时标记已读
  if (peerId.value) {
    markChatRead({ fromUserId: peerId.value }).catch(() => {})
  }
})
</script>

<style scoped>
.chat-body {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
}
.msg-box {
  flex: 1;
  min-height: 400px;
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
.msg-row.me {
  margin-left: auto;
  text-align: right;
}
.msg-row.other {
  margin-right: auto;
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
.msg-row.me .msg-content {
  background: #409EFF;
  color: #fff;
}
.msg-row.other .msg-content {
  background: #fff;
  border: 1px solid #e4e7ed;
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
