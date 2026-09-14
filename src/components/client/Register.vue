<template>
  <div class="login-page">
    <transition name="fade">
      <div v-show="isShow" class="error-tip">
        <el-icon class="tip-icon"><CircleCloseFilled /></el-icon>
        <span>{{ errMessage }}</span>
      </div>
    </transition>

    <el-card class="login-card" shadow="hover">
      <div class="card-header">
        <el-icon color="#409EFF" size="42"><Key /></el-icon>
        <h1 class="login-title">注册</h1>
      </div>

      <el-form label-width="0px" class="login-form">
        <el-form-item>
          <el-input v-model="phone" placeholder="请输入手机号" size="large" prefix-icon="User" clearable/>
        </el-form-item>

        <el-form-item class="code-row">
          <el-input v-model="code" placeholder="请输入验证码" size="large" prefix-icon="Document" clearable/>
          <el-button type="primary" size="large" class="code-btn" :disabled="time > 0 || sending" @click="getCode">
            {{ time <= 0 ? '获取验证码' : `${time}秒后重发` }}
          </el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="checking" @click="next">下 一 步</el-button>
        </el-form-item>
      </el-form>

      <div class="link-row">
        <router-link to="/clientLogin" class="forget-link">← 返回登录页</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerGetCode, registerCheckCode } from '../../api/user'
import { CircleCloseFilled, Key, User, Document } from '@element-plus/icons-vue'

const router = useRouter()

const phone = ref('')
const code = ref('')
const time = ref(0)
let timer = null

const errMessage = ref('')
const isShow = ref(false)
const sending = ref(false)
const checking = ref(false)

const showError = (msg) => {
  errMessage.value = msg
  isShow.value = true
  setTimeout(() => { isShow.value = false }, 2000)
}

const getCode = async () => {
  if (!phone.value) {
    showError('请先输入手机号')
    return
  }
  sending.value = true
  try {
    await registerGetCode({ phone: phone.value })
    ElMessage.success('验证码已发送，请注意查收')
    if (timer) clearInterval(timer)
    time.value = 60
    timer = setInterval(() => {
      time.value--
      if (time.value <= 0) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (err) {
    showError(err.message || '验证码发送失败')
  } finally {
    sending.value = false
  }
}

const next = async () => {
  if (!phone.value || !code.value) {
    showError('手机号和验证码不能为空')
    return
  }
  checking.value = true
  try {
    await registerCheckCode({ phone: phone.value, checkCode: code.value })
    router.push({
      path: '/clientSetAccount',
      query: { phone: phone.value }
    })
  } catch (err) {
    showError(err.message || '验证码错误')
  } finally {
    checking.value = false
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
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

.code-row {
  display: flex;
  gap: 12px;
  align-items: stretch;
}
.code-row :deep(.el-input) {
  flex: 1;
}
.code-btn {
  white-space: nowrap;
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
