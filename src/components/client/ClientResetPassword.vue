<template>
  <div class="reset-page">
    <transition name="fade">
      <div v-show="isShow" class="error-tip">
        <el-icon class="tip-icon"><CircleCloseFilled /></el-icon>
        <span>{{ errMessage }}</span>
      </div>
    </transition>

    <el-card class="login-card" shadow="hover">
      <div class="card-header">
        <el-icon color="#409EFF" size="42"><Key /></el-icon>
        <h1 class="login-title">重置新密码</h1>
      </div>

      <el-form label-width="0px" class="login-form">
        <el-input type="hidden" v-model="phone" />

        <el-form-item>
          <el-input v-model="password" type="password" placeholder="请输入新密码" size="large" prefix-icon="Lock" show-password clearable />
        </el-form-item>

        <el-form-item>
          <el-input v-model="againPassword" type="password" placeholder="请输入确认密码" size="large" prefix-icon="Lock" show-password clearable/>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="submitting" @click="updatePwd">确 定</el-button>
        </el-form-item>
      </el-form>

      <div class="link-row">
        <router-link to="/clientForgetPassword" class="back-link">← 返回验证码页</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { updatePassword } from '../../api/user'
import { CircleCloseFilled, Key, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const phone = ref(route.query.phone)
const password = ref('')
const againPassword = ref('')
const submitting = ref(false)

const errMessage = ref('')
const isShow = ref(false)

const showError = (msg) => {
  errMessage.value = msg
  isShow.value = true
  setTimeout(() => { isShow.value = false }, 2000)
}

const updatePwd = async () => {
  if (!password.value || !againPassword.value) {
    showError('密码不能为空')
    return
  }
  if (password.value !== againPassword.value) {
    showError('两次密码输入不一致')
    return
  }
  submitting.value = true
  try {
    await updatePassword({
      phone: phone.value,
      newPassword: password.value,
      againPassword: againPassword.value
    })
    ElMessage.success('密码修改成功，请重新登录')
    router.push('/clientLogin')
  } catch (err) {
    showError(err.message || '密码修改失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.reset-page {
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
.back-link {
  color: #409EFF;
  font-size: 14px;
  text-decoration: none;
}
.back-link:hover {
  text-decoration: underline;
}
</style>
