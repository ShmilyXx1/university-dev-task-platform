import axios from 'axios'
import { useUserStore } from '../stores/user'
import router from '../router'
import { ElMessage } from 'element-plus'

// 创建 axios 实例（baseURL 留空，通过 Vite 代理转发到后端，避免跨域）
const service = axios.create({
  baseURL: '',
  timeout: 10000
})

// 统一处理"登录失效"：清除登录态 + 跳转登录页（401 响应和本地预判过期共用）
function handleAuthExpired() {
  const userStore = useUserStore()
  userStore.logout()
  ElMessage.error('登录已过期，请重新登录')
  const currentPath = router.currentRoute.value.path
  // 已经在登录页就不再跳转，避免循环
  if (currentPath.includes('Login')) return
  // 客户端页面（含支付结果页）回客户端登录
  if (currentPath.startsWith('/client') || currentPath.startsWith('/payResult')) {
    router.push('/clientLogin')
  } else {
    router.push('/login')
  }
}

// 请求拦截器：自动携带 token
const AUTH_PATHS = ['/user/adminLogin', '/user/userLogin', '/user/register', '/user/registerGetCode', '/user/registerCheckCode', '/user/forgetPasswordGetCode', '/user/forgetPasswordCheckCode', '/user/updatePassword']

service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    // 登录/注册/重置密码等接口不需要 token
    const needAuth = !AUTH_PATHS.some(p => config.url && config.url.includes(p))
    if (userStore.token && needAuth) {
      // ===== 发请求前本地预判：token 已过期就不要再发，直接清登录态跳登录 =====
      if (userStore.isTokenExpired) {
        handleAuthExpired()
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
      config.headers['token'] = userStore.token
      config.headers['Authorization'] = `Bearer ${userStore.token}`
      if (config.params) {
        config.params.token = userStore.token
      } else {
        config.params = { token: userStore.token }
      }
      // 如果是 POST/PUT 请求有 body，也在 body 中添加 token
      if ((config.method === 'post' || config.method === 'put') && config.data && typeof config.data === 'object') {
        config.data.token = userStore.token
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理业务码
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        handleAuthExpired()
        return Promise.reject(new Error(res.msg || res.message || '登录已过期，请重新登录'))
      }
      ElMessage.error(res.msg || res.message || '请求失败')
      return Promise.reject(new Error(res.msg || res.message || 'Error'))
    }
    return res
  },
  (error) => {
    // HTTP 401 → 未登录/过期 → 清除登录态跳登录
    if (error.response && error.response.status === 401) {
      handleAuthExpired()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    // HTTP 403 → 已登录但无权限 → 只提示，不清 token
    if (error.response && error.response.status === 403) {
      ElMessage.error('无权限访问该接口')
      return Promise.reject(error)
    }
    // 请求被本地拦截器主动拒绝（token 本地预判过期），不再重复弹提示
    if (error.message === '登录已过期，请重新登录') {
      return Promise.reject(error)
    }
    // 后端业务层返回 code=401（LoginInterceptor 验证失败）
    if (error.response?.data?.code === 401) {
      handleAuthExpired()
      return Promise.reject(error)
    }
    // 后端业务层返回 code=403（无权限）
    if (error.response?.data?.code === 403) {
      ElMessage.error(error.response.data.msg || '无权限访问该接口')
      return Promise.reject(error)
    }
    // 兜底：其他错误
    ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default service
