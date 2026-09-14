import { ElMessage } from 'element-plus'

// 客服聊天 + P2P 聊天 WebSocket 封装（原生 WebSocket，自动重连）
let ws = null
let listeners = {}   // type -> [回调]
let reconnectTimer = null
let manualClose = false
let globalInitDone = false

/**
 * 建立聊天 WebSocket 连接
 * @param {string} token JWT
 */
export function connectChat(token) {
  if (!token) return null
  manualClose = false
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return ws
  }
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  // 开发环境经 Vite 代理（/ws 配置了 ws:true），生产环境同源
  ws = new WebSocket(`${proto}://${location.host}/ws/chat/${encodeURIComponent(token)}`)

  ws.onmessage = (event) => {
    let msg
    try {
      msg = JSON.parse(event.data)
    } catch (e) {
      return
    }
    const cbs = listeners[msg.type]
    if (cbs) cbs.forEach((cb) => cb(msg))
    const all = listeners['*']
    if (all) all.forEach((cb) => cb(msg))
  }

  ws.onclose = () => {
    ws = null
    if (!manualClose) {
      // 3 秒自动重连
      reconnectTimer = setTimeout(() => connectChat(token), 3000)
    }
  }

  ws.onerror = () => {
    // 错误后浏览器会自动触发 onclose，由 onclose 负责重连
  }

  return ws
}

/**
 * 【全局初始化】应用启动/登录后调用一次
 * 建立连接 + 注册未读消息通知监听
 */
export function initChatWs(token) {
  if (!token) return
  globalInitDone = true
  connectChat(token)
  // 全局监听：未读数变化 → 更新 store
  onChat('notify_unread', (msg) => {
    try {
      // 动态引入避免循环依赖
      import('../stores/chat').then(({ useChatStore }) => {
        const store = useChatStore()
        store.updateFromWs(msg.totalUnread || 0)
      })
    } catch (e) {}
  })
  // 全局监听：收到新聊天消息（非当前聊天页面时弹提示）
  onChat('*', (msg) => {
    // notify_unread 已单独处理
    if (msg.type === 'notify_unread') return
    // 如果是聊天消息但当前不在聊天页面 → 弹轻提示
    if (msg.type === 'chat' && !isOnChatPage()) {
      const fromName = msg.peerName || `用户${msg.fromUserId || ''}`
      // P2P 消息：fromUserId 是发送方，自己发的不弹
      // 客服消息：from === 'user' 表示用户发的（客服端弹），from === 'service' 表示客服发的（用户端弹）
      let isFromOther = false
      if (msg.peerType === 'p2p') {
        const myUserId = Number(localStorage.getItem('userId')) || 0
        isFromOther = msg.fromUserId && Number(msg.fromUserId) !== myUserId
      } else {
        isFromOther = msg.from === 'user' || msg.from === 'service'
      }
      if (isFromOther) {
        ElMessage({
          message: `${fromName}：${(msg.content || '').substring(0, 30)}`,
          type: 'info',
          duration: 3000,
          showClose: true
        })
      }
    }
  })
}

function isOnChatPage() {
  const path = location.pathname
  return path.includes('/clientChat')
    || path.includes('/ServiceMain')
    || path.includes('/serviceMain')
    || path.includes('/clientP2PChat')
}

/**
 * 发送消息
 * @param {string} type 消息类型
 * @param {object} data 附加字段
 */
export function sendChat(type, data = {}) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type, ...data }))
    return true
  }
  return false
}

/**
 * 监听某类消息
 * @param {string} type 消息类型，'*' 表示所有消息
 * @param {function} cb 回调
 */
export function onChat(type, cb) {
  if (!listeners[type]) listeners[type] = []
  listeners[type].push(cb)
}

/**
 * 移除监听
 */
export function offChat(type, cb) {
  if (!listeners[type]) return
  if (cb) {
    listeners[type] = listeners[type].filter((fn) => fn !== cb)
  } else {
    listeners[type] = []
  }
}

/**
 * 主动关闭（不再重连）
 */
export function closeChat() {
  manualClose = true
  globalInitDone = false
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (ws) {
    ws.close()
    ws = null
  }
  listeners = {}
}

/**
 * 连接是否已打开
 */
export function isChatOpen() {
  return ws && ws.readyState === WebSocket.OPEN
}
