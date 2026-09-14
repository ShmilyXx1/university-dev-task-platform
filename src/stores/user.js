import { defineStore } from 'pinia'

export const DEFAULT_AVATAR = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

function decodeJWT(token) {
  if (!token) return {}
  try {
    const payload = token.split('.')[1]
    // atob 按 Latin-1 逐字节解码，JWT 载荷是 UTF-8 JSON（含中文用户名时会乱码），
    // 必须先转成字节数组再用 TextDecoder 按 UTF-8 解码
    const binary = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
    const json = new TextDecoder('utf-8').decode(bytes)
    return JSON.parse(json)
  } catch {
    return {}
  }
}

export const useUserStore = defineStore('user', {
  state: () => {
    const savedToken = localStorage.getItem('token') || ''
    // 优先从 JWT 载荷取用户名（登录后令牌是最新鲜的权威数据，
    // 同时可自愈旧版本 atob 解码错误后持久化到 localStorage 的乱码用户名）
    let finalUsername = ''
    let finalUserId = 0
    if (savedToken) {
      const payload = decodeJWT(savedToken)
      if (payload.username) {
        finalUsername = payload.username
        localStorage.setItem('username', finalUsername)
      }
      if (payload.userId) {
        finalUserId = Number(payload.userId) || 0
      }
    }
    if (!finalUsername) {
      finalUsername = localStorage.getItem('username') || ''
    }
    return {
      token: savedToken,
      isLogin: !!savedToken,
      role: localStorage.getItem('role') || '',
      username: finalUsername,
      userId: finalUserId,
      imagePath: localStorage.getItem('imagePath') || ''
    }
  },
  getters: {
    avatarUrl(state) {
      return state.imagePath || DEFAULT_AVATAR
    },
    // token 是否已过期（前端本地校验 JWT 的 exp，避免拿着过期 token 发请求）
    isTokenExpired(state) {
      if (!state.token) return true
      const payload = decodeJWT(state.token)
      if (!payload.exp) return false
      // exp 是秒级时间戳；提前 5 秒判定，规避临界时间差
      return payload.exp * 1000 <= Date.now() + 5000
    }
  },
  actions: {
    setToken(token) {
      this.token = token
      this.isLogin = true
      localStorage.setItem('token', token)
      // 同步把 userId 持久化（从 JWT payload 解码），方便 ws.js 等工具模块判断身份
      if (token) {
        const payload = decodeJWT(token)
        if (payload.userId) {
          this.userId = Number(payload.userId) || 0
          localStorage.setItem('userId', String(this.userId))
        }
      } else {
        this.userId = 0
        localStorage.removeItem('userId')
      }
      // 登录后自动建立聊天 WebSocket 连接（异步，不阻塞）
      if (token) {
        import('../utils/ws').then(({ initChatWs }) => {
          initChatWs(token)
        })
      }
    },

    setUsername(username) {
      this.username = username
      localStorage.setItem('username', username)
    },

    setImagePath(imagePath) {
      this.imagePath = imagePath
      localStorage.setItem('imagePath', imagePath)
    },

    setRole(role) {
      this.role = role
      localStorage.setItem('role', role)
    },

    logout() {
      this.token = ''
      this.isLogin = false
      this.role = ''
      this.username = ''
      this.userId = 0
      this.imagePath = ''
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('username')
      localStorage.removeItem('userId')
      localStorage.removeItem('imagePath')
      // 关闭聊天 WebSocket
      import('../utils/ws').then(({ closeChat }) => {
        closeChat()
      })
    }
  }
})
