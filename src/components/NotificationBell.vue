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
        <span style="font-weight:600">消息通知</span>
        <el-button link size="small" @click="markAll" :disabled="!chatStore.hasUnread">全部已读</el-button>
      </div>
      <div v-if="chatStore.details.length === 0" class="notif-empty">
        <el-icon :size="36" color="#c0c4cc"><Bell /></el-icon>
        <div style="margin-top:8px;color:#909399;font-size:13px">暂无新消息</div>
      </div>
      <div v-else class="notif-list">
        <div
          v-for="item in chatStore.details"
          :key="item.fromUserId"
          class="notif-item"
          @click="goChat(item)"
        >
          <el-avatar :size="32" style="background:#409EFF;margin-right:10px">
            {{ (item.username || 'U').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="notif-info">
            <div class="notif-name">{{ item.username || '用户' + item.fromUserId }}</div>
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
import { markChatRead } from '../api/chat'

const router = useRouter()
const chatStore = useChatStore()
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
  // 跳转到 P2P 聊天页，带上对方 userId 和用户名
  router.push({
    path: '/clientP2PChat',
    query: { peerId: item.fromUserId, peerName: item.username }
  })
}

onMounted(() => {
  chatStore.fetchUnread()
})
</script>

<style scoped>
.bell-trigger {
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
}
.bell-trigger:hover {
  background: #f0f2f5;
}
.bell-icon {
  color: #606266;
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
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 4px;
}
.notif-empty {
  text-align: center;
  padding: 30px 0;
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
  background: #f5f7fa;
}
.notif-info {
  flex: 1;
  min-width: 0;
}
.notif-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.notif-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>
