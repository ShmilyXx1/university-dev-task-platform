<template>
  <el-popover
    placement="bottom-end"
    :width="320"
    trigger="click"
    v-model:visible="visible"
    @show="onPopoverShow"
  >
    <template #reference>
      <el-badge :value="chatStore.totalUnread" :max="99" :hidden="!chatStore.hasUnread" class="bell-trigger">
        <el-icon :size="22" class="bell-icon"><Bell /></el-icon>
      </el-badge>
    </template>

    <div class="notif-panel">
      <div class="notif-header">
        <span class="notif-title">消息通知</span>
        <el-button link size="small" @click="markAll" :disabled="!chatStore.hasUnread">全部已读</el-button>
      </div>
      <div v-if="chatStore.details.length === 0" class="notif-empty">
        <el-icon :size="36" color="var(--text-muted)"><Bell /></el-icon>
        <div class="notif-empty-text">暂无新消息</div>
      </div>
      <div v-else class="notif-list">
        <div
          v-for="item in chatStore.details"
          :key="item.fromUserId"
          class="notif-item"
          @click="goChat(item)"
        >
          <el-avatar :size="32" class="notif-avatar">
            {{ (item.username || 'U').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="notif-info">
            <div class="notif-name">
              {{ item.username || '用户' + item.fromUserId }}
              <el-tag v-if="item.msgType === 'service'" size="small" type="warning" effect="plain">客服</el-tag>
            </div>
            <div class="notif-sub">有 {{ item.unreadCount }} 条新消息</div>
          </div>
          <el-badge :value="item.unreadCount" :max="99" />
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { useChatStore } from '../stores/chat'
import { useUserStore } from '../stores/user'
import { connectChat } from '../utils/ws'
import { markChatRead } from '../api/chat'

const router = useRouter()
const chatStore = useChatStore()
const userStore = useUserStore()
const visible = ref(false)

const onPopoverShow = () => {
  // 每次打开刷新一次未读详情
  chatStore.fetchUnread()
}

const markAll = async () => {
  try {
    await markChatRead({})
    chatStore.markAllRead()
    ElMessage.success('已全部标记为已读')
  } catch (e) {}
}

const goChat = async (item) => {
  // 标记该发送者已读
  try { await markChatRead({ fromUserId: item.fromUserId }) } catch (e) {}
  chatStore.markPeerRead(item.fromUserId)
  visible.value = false
  // 私信 → P2P 页；客服消息 → 客服/管理员进客服工作台，普通用户进客户端客服会话
  if (item.msgType === 'p2p') {
    router.push({
      path: '/clientP2PChat',
      query: { peerId: item.fromUserId, peerName: item.username }
    })
  } else if (userStore.role === '客服' || userStore.role === '管理员') {
    router.push('/serviceMain')
  } else {
    router.push('/clientChat')
  }
}

onMounted(() => {
  chatStore.fetchUnread()
  // 兜底：确保聊天 WebSocket 已连接（铃铛挂载于各端主页，刷新/冷启动后都能重连）
  if (userStore.token) connectChat(userStore.token)
})
</script>

<style scoped>
.bell-trigger {
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: background-color 0.2s;
}
.bell-trigger:hover {
  background: var(--primary-light);
}
.bell-icon {
  color: var(--text-regular);
  transition: color 0.2s;
}
.bell-trigger:hover .bell-icon {
  color: var(--primary);
}
.notif-panel {
  max-height: 400px;
  overflow-y: auto;
}
.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 4px;
}
.notif-title {
  font-weight: 600;
}
.notif-empty {
  text-align: center;
  padding: 30px 0;
}
.notif-empty-text {
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 13px;
}
.notif-list {
  display: flex;
  flex-direction: column;
}
.notif-item {
  display: flex;
  align-items: center;
  padding: 10px 4px;
  cursor: pointer;
  border-radius: 6px;
}
.notif-item:hover {
  background: var(--content-bg);
}
.notif-avatar {
  background: var(--gradient-primary);
  margin-right: 10px;
}
.notif-info {
  flex: 1;
  min-width: 0;
}
.notif-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}
.notif-sub {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}
</style>
