<template>
  <div class="login-page">
    <!-- 错误提示弹窗 -->
    <transition name="fade">
      <div v-show="isShow" class="error-tip">
        <el-icon class="tip-icon"><CircleCloseFilled /></el-icon>
        <span>{{ errMessage }}</span>
      </div>
    </transition>

    <el-card class="login-card" shadow="hover">
      <div class="card-header">
        <el-icon class="login-icon" color="#409EFF" size="42"><UserFilled /></el-icon>
        <h1 class="login-title">系统登录</h1>
      </div>

      <el-form ref="loginFormRef" :model="loginForm" label-width="0px" class="login-form">
        <el-form-item>
          <el-input v-model="loginForm.phone" placeholder="请输入账号或手机号" size="large" prefix-icon="User" clearable/>
        </el-form-item>

        <el-form-item>
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large" prefix-icon="Lock" show-password clearable/>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="login">登 录</el-button>
        </el-form-item>
      </el-form>

      <div class="link-row">
        <router-link to="/getCode" class="forget-link">忘记密码？</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { login as loginApi } from '../api/user'
import { UserFilled, CircleCloseFilled, User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const loginForm = ref({
  phone: '',
  password: ''
})
const errMessage = ref('')
const isShow = ref(false)
const loading = ref(false)

const showError = (msg) => {
  errMessage.value = msg
  isShow.value = true
  setTimeout(() => { isShow.value = false }, 2000)
}

// 后端返回中文角色名，映射到路由
const roleRouteMap = {
  '管理员': '/adminMain',
  '审核员': '/auditMain',
  '客服': '/serviceMain'
}

const login = async () => {
  if (!loginForm.value.phone || !loginForm.value.password) {
    showError('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res = await loginApi({
      phone: loginForm.value.phone,
      password: loginForm.value.password
    })
    // 存储token和角色
    userStore.setToken(res.data.token)
    const role = res.data.role
    userStore.setRole(role)
    router.push(roleRouteMap[role] || '/adminMain')
  } catch (err) {
    showError(err.message || '账号或者密码错误')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f4ff 0%, #e6edff 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
}

.error-tip {
  position: fixed;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  background-color: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
  padding: 10px 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 999;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.login-card {
  width: 420px;
  border-radius: 16px;
  padding: 35px 30px;
  background-color: #ffffff;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 30px;
}
.login-title {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.login-form {
  margin-bottom: 20px;
}
:deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  border-radius: 8px;
}

.link-row {
  text-align: right;
}
.forget-link {
  color: #409EFF;
  font-size: 14px;
  text-decoration: none;
}
.forget-link:hover {
  text-decoration: underline;
}
</style>
