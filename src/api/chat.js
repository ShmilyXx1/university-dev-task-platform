import request from '../utils/request'

/**
 * 查询与某人的聊天历史
 * GET /chat/history?peerId=对方userId
 */
export function getChatHistory(params) {
  return request({ url: '/chat/history', method: 'get', params })
}

/**
 * 查询未读消息统计（按发送者分组）
 * GET /chat/unread
 */
export function getChatUnread() {
  return request({ url: '/chat/unread', method: 'get' })
}

/**
 * 标记消息已读
 * PUT /chat/read?fromUserId=xxx 或不带参数全部标记
 */
export function markChatRead(params) {
  return request({ url: '/chat/read', method: 'put', params })
}
