import { defineStore } from 'pinia'
import { getChatUnread } from '../api/chat'

/**
 * 全局未读消息通知状态
 * 所有端（客户端 / 客服 / 审核 / 管理）共享一个 store
 */
export const useChatStore = defineStore('chat', {
  state: () => ({
    totalUnread: 0,          // 总未读数
    details: [],             // [{ fromUserId, username, unreadCount }]
    initialized: false
  }),
  getters: {
    hasUnread: (state) => state.totalUnread > 0
  },
  actions: {
    /** 启动时从 REST 接口拉一次 */
    async fetchUnread() {
      try {
        const res = await getChatUnread()
        this.totalUnread = res.data?.totalUnread || 0
        this.details = res.data?.details || []
        this.initialized = true
      } catch (e) {
        // 未登录或接口失败静默忽略
      }
    },
    /** WebSocket 推送更新（只更新总数，下拉列表需要时再拉详情） */
    updateFromWs(total) {
      this.totalUnread = total || 0
    },
    /** 点击"全部标记已读" */
    markAllRead() {
      this.totalUnread = 0
      this.details = []
    },
    /** 某条会话已读后减少计数 */
    markPeerRead(fromUserId) {
      const item = this.details.find((d) => d.fromUserId === fromUserId)
      if (item) {
        this.totalUnread = Math.max(0, this.totalUnread - item.unreadCount)
        item.unreadCount = 0
      }
    }
  }
})
